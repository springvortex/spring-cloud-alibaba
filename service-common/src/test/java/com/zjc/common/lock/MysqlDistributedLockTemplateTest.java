package com.zjc.common.lock;

import com.zjc.common.exception.BusinessException;
import com.zjc.common.lock.DistributedLockProperties.Mysql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MysqlDistributedLockTemplate} 行为测试。
 *
 * <p>使用内存中的 JDBC 假实现验证获取、重入、抢占和释放逻辑，不依赖真实 MySQL。
 *
 * @author jiancai.zhong
 */
@DisplayName("MySQL 分布式锁模板")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MysqlDistributedLockTemplateTest {

    /**
     * 测试锁 key。
     */
    private static final String LOCK_KEY = "zjc:test:lock:1";

    /**
     * 测试锁等待时间。
     */
    private static final Duration WAIT_TIME = Duration.ofMillis(100);

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private InMemoryJdbc jdbc;

    private MysqlDistributedLockTemplate lockTemplate;

    /**
     * 初始化真实事务模板和内存 JDBC 实现。
     */
    @BeforeEach
    void setUp() {
        jdbc = new InMemoryJdbc();
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(transactionStatus);
        lockTemplate = new MysqlDistributedLockTemplate(
                jdbc,
                new TransactionTemplate(transactionManager),
                mysqlConfig());
    }

    /**
     * 关闭续期线程池，避免测试线程泄漏。
     */
    @AfterEach
    void tearDown() {
        lockTemplate.close();
    }

    /**
     * 验证首次获取锁会初始化锁行，业务结束后删除锁行。
     */
    @Test
    @DisplayName("execute: 首次获取锁并在业务结束后释放")
    void acquiresAndReleasesNewLock() {
        String result = lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> {
            assertThat(jdbc.locks).containsKey(LOCK_KEY);
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(jdbc.locks).doesNotContainKey(LOCK_KEY);
        assertThat(jdbc.calls)
                .extracting(SqlCall::type)
                .containsExactly(SqlType.INSERT, SqlType.DELETE);
    }

    /**
     * 验证同一线程嵌套获取同一把锁时增加重入计数，外层结束才删除锁行。
     */
    @Test
    @DisplayName("execute: 支持同线程重入")
    void supportsReentrantLocking() {
        String result = lockTemplate.execute(LOCK_KEY, WAIT_TIME, () ->
                lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> {
                    assertThat(jdbc.locks.get(LOCK_KEY).count()).isEqualTo(2);
                    return "inner";
                }));

        assertThat(result).isEqualTo("inner");
        assertThat(jdbc.locks).doesNotContainKey(LOCK_KEY);
        assertThat(jdbc.calls)
                .extracting(SqlCall::type)
                .containsExactly(SqlType.INSERT, SqlType.REENTRANT,
                        SqlType.DECREASE, SqlType.DELETE);
    }

    /**
     * 验证并发初始化唯一键冲突会被当作抢锁失败，后续继续等待而不是直接抛数据库异常。
     */
    @Test
    @DisplayName("execute: 插入唯一键冲突后继续重试")
    void retriesAfterConcurrentInsertConflict() {
        jdbc.locks.put(LOCK_KEY, new LockState("another-owner", 1, false));
        jdbc.hideExistingLockOnce.set(true);
        jdbc.failNextInsert.set(true);

        assertThatThrownBy(() -> lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("操作繁忙，请稍后再试");

        assertThat(jdbc.calls)
                .extracting(SqlCall::type)
                .containsExactly(SqlType.INSERT_CONFLICT);
        assertThat(jdbc.locks.get(LOCK_KEY).owner()).isEqualTo("another-owner");
    }

    /**
     * 验证租约过期后可以被其他持有者抢占。
     */
    @Test
    @DisplayName("execute: 租约过期后允许抢占")
    void takesOverExpiredLock() {
        jdbc.locks.put(LOCK_KEY, new LockState("old-owner", 1, true));

        String result = lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> {
            assertThat(jdbc.locks.get(LOCK_KEY).owner()).isNotEqualTo("old-owner");
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(jdbc.locks).doesNotContainKey(LOCK_KEY);
        assertThat(jdbc.calls)
                .extracting(SqlCall::type)
                .containsExactly(SqlType.TAKEOVER_EXPIRED, SqlType.DELETE);
    }

    /**
     * 验证业务异常会继续向上传递，并且锁不会留在数据库中。
     */
    @Test
    @DisplayName("execute: 业务异常时仍释放锁")
    void releasesLockWhenActionThrows() {
        IllegalStateException expected = new IllegalStateException("business failed");

        assertThatThrownBy(() -> lockTemplate.execute(LOCK_KEY, WAIT_TIME, () -> {
            throw expected;
        })).isSameAs(expected);

        assertThat(jdbc.locks).doesNotContainKey(LOCK_KEY);
    }

    /**
     * 验证表名配置只允许安全字符，避免 SQL 拼接注入。
     */
    @Test
    @DisplayName("构造函数: 拒绝不安全表名")
    void rejectsUnsafeTableName() {
        Mysql config = mysqlConfig();
        config.setTableName("t_lock; drop table goods");

        assertThatThrownBy(() -> new MysqlDistributedLockTemplate(
                jdbc, new TransactionTemplate(transactionManager), config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("table name is invalid");
    }

    /**
     * 构造快速重试的 MySQL 锁配置。
     *
     * @return MySQL 锁配置
     */
    private static Mysql mysqlConfig() {
        Mysql config = new Mysql();
        config.setLeaseTime(Duration.ofMillis(100));
        config.setRetryInterval(Duration.ofMillis(1));
        return config;
    }

    /**
     * SQL 调用类型。
     */
    private enum SqlType {
        INSERT, REENTRANT, TAKEOVER_EXPIRED, RENEW, DECREASE, DELETE, INSERT_CONFLICT
    }

    /**
     * 记录一次 SQL 更新调用。
     *
     * @param type       SQL 类型
     * @param sql        SQL 语句
     * @param parameters 绑定参数
     */
    private record SqlCall(SqlType type, String sql, Object[] parameters) {
    }

    /**
     * 内存锁行状态。
     *
     * @param owner   持有者
     * @param count   重入次数
     * @param expired 租约是否过期
     */
    private record LockState(String owner, int count, boolean expired) {
    }

    /**
     * 模拟 MySQL 锁表行为的 JDBC 对象。
     */
    private static final class InMemoryJdbc extends JdbcTemplate {

        private final Map<String, LockState> locks = new HashMap<>();

        private final List<SqlCall> calls = new ArrayList<>();

        private final AtomicBoolean hideExistingLockOnce = new AtomicBoolean(false);

        private final AtomicBoolean failNextInsert = new AtomicBoolean(false);

        /**
         * 读取锁行；可按测试要求让第一次读取表现为行不存在，模拟并发插入窗口。
         */
        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            if (hideExistingLockOnce.compareAndSet(true, false)) {
                return List.of();
            }
            LockState state = locks.get((String) args[0]);
            if (state == null) {
                return List.of();
            }
            try {
                return List.of(rowMapper.mapRow(resultSet(state), 0));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * 执行插入、重入、抢占、续期和释放 SQL 的等价内存行为。
         */
        @Override
        public int update(String sql, Object... args) {
            if (sql.contains("INSERT INTO")) {
                String key = (String) args[0];
                if (failNextInsert.compareAndSet(true, false)) {
                    calls.add(new SqlCall(SqlType.INSERT_CONFLICT, sql, args));
                    throw new DuplicateKeyException("duplicate lock key");
                }
                calls.add(new SqlCall(SqlType.INSERT, sql, args));
                locks.put(key, new LockState((String) args[1], 1, false));
                return 1;
            }
            if (sql.contains("lock_count = lock_count + 1")) {
                String key = (String) args[1];
                calls.add(new SqlCall(SqlType.REENTRANT, sql, args));
                return updateState(key, state -> new LockState(state.owner(), state.count() + 1, false));
            }
            if (sql.contains("SET lock_owner = ?")) {
                String key = (String) args[2];
                calls.add(new SqlCall(SqlType.TAKEOVER_EXPIRED, sql, args));
                locks.put(key, new LockState((String) args[0], 1, false));
                return 1;
            }
            if (sql.contains("lock_count = lock_count - 1")) {
                String key = (String) args[0];
                calls.add(new SqlCall(SqlType.DECREASE, sql, args));
                return updateState(key, state -> new LockState(state.owner(), state.count() - 1, false));
            }
            if (sql.startsWith("DELETE")) {
                String key = (String) args[0];
                calls.add(new SqlCall(SqlType.DELETE, sql, args));
                LockState state = locks.get(key);
                if (state == null || !state.owner().equals(args[1])) {
                    return 0;
                }
                locks.remove(key);
                return 1;
            }
            if (sql.contains("expire_at = DATE_ADD")) {
                String key = (String) args[1];
                calls.add(new SqlCall(SqlType.RENEW, sql, args));
                return updateState(key, state -> new LockState(state.owner(), state.count(), false));
            }
            throw new IllegalArgumentException("Unexpected update sql: " + sql);
        }

        /**
         * 更新锁状态。
         *
         * @param key     锁 key
         * @param updater 状态更新函数
         * @return 是否更新成功
         */
        private int updateState(String key, UnaryOperator<LockState> updater) {
            LockState state = locks.get(key);
            if (state == null) {
                return 0;
            }
            locks.put(key, updater.apply(state));
            return 1;
        }

        /**
         * 构造 RowMapper 需要的结果集。
         *
         * @param state 锁状态
         * @return Mock 结果集
         * @throws SQLException 创建失败时抛出
         */
        private static ResultSet resultSet(LockState state) throws SQLException {
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getString("lock_owner")).thenReturn(state.owner());
            when(resultSet.getInt("lock_count")).thenReturn(state.count());
            when(resultSet.getBoolean("expired")).thenReturn(state.expired());
            return resultSet;
        }
    }
}
