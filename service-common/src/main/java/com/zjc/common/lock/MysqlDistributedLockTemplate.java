package com.zjc.common.lock;

import com.zjc.common.constant.ApiResponseEnum;
import com.zjc.common.exception.BusinessException;
import com.zjc.common.lock.DistributedLockProperties.Mysql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * MySQL 租约表分布式锁实现。
 *
 * <p>使用唯一键行锁实现互斥，保存线程级 owner 和重入次数；业务执行期间由后台线程
 * 周期性延长租期，异常退出或进程宕机后租期到期自动释放。
 *
 * @author jiancai.zhong
 */
@Slf4j
public class MysqlDistributedLockTemplate implements DistributedLockTemplate, AutoCloseable {

    /**
     * 锁等待超时时返回给调用方的业务错误码。
     */
    private static final int LOCK_TIMEOUT_CODE = 503;

    /**
     * 默认锁等待超时提示。
     */
    private static final String DEFAULT_TIMEOUT_MESSAGE = "操作繁忙，请稍后再试";

    /**
     * 锁表名安全字符规则，防止配置拼接 SQL。
     */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)?$");

    /**
     * 每个线程一个 owner，用于识别锁持有者和可重入调用。
     */
    private static final ThreadLocal<String> LOCK_OWNERS = ThreadLocal.withInitial(() ->
            UUID.randomUUID() + ":" + Thread.currentThread().threadId());

    /**
     * 续期线程编号。
     */
    private static final AtomicLong RENEWAL_THREAD_ID = new AtomicLong();

    private final JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    private final String tableName;

    private final Duration leaseTime;

    private final Duration retryInterval;

    private final ScheduledExecutorService renewalExecutor;

    private final String selectLockSql;

    private final String insertLockSql;

    private final String updateReentrantSql;

    private final String updateExpiredSql;

    private final String renewSql;

    private final String decreaseCountSql;

    private final String deleteLockSql;

    /**
     * 创建 MySQL 分布式锁模板。
     *
     * @param jdbcTemplate        JDBC 操作对象
     * @param transactionTemplate 事务模板
     * @param mysql               MySQL 锁配置
     */
    public MysqlDistributedLockTemplate(JdbcTemplate jdbcTemplate,
                                        TransactionTemplate transactionTemplate,
                                        Mysql mysql) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate,
                "transactionTemplate must not be null");
        Objects.requireNonNull(mysql, "mysql config must not be null");
        this.tableName = safeTableName(mysql.getTableName());
        this.leaseTime = positive(mysql.getLeaseTime(), "leaseTime");
        this.retryInterval = positive(mysql.getRetryInterval(), "retryInterval");
        this.renewalExecutor = Executors.newScheduledThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors()),
                task -> {
                    Thread thread = new Thread(task, "mysql-lock-renewal-"
                            + RENEWAL_THREAD_ID.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        this.selectLockSql = "SELECT lock_owner, lock_count, expire_at < NOW(3) AS expired FROM "
                + tableName + " WHERE lock_key = ? FOR UPDATE";
        this.insertLockSql = "INSERT INTO " + tableName
                + " (lock_key, lock_owner, lock_count, expire_at) VALUES (?, ?, 1, "
                + "DATE_ADD(NOW(3), INTERVAL ? MICROSECOND))";
        this.updateReentrantSql = "UPDATE " + tableName
                + " SET lock_count = lock_count + 1, expire_at = DATE_ADD(NOW(3), INTERVAL ? MICROSECOND) "
                + "WHERE lock_key = ?";
        this.updateExpiredSql = "UPDATE " + tableName
                + " SET lock_owner = ?, lock_count = 1, expire_at = DATE_ADD(NOW(3), INTERVAL ? MICROSECOND) "
                + "WHERE lock_key = ?";
        this.renewSql = "UPDATE " + tableName
                + " SET expire_at = DATE_ADD(NOW(3), INTERVAL ? MICROSECOND) "
                + "WHERE lock_key = ? AND lock_owner = ?";
        this.decreaseCountSql = "UPDATE " + tableName
                + " SET lock_count = lock_count - 1 WHERE lock_key = ? AND lock_owner = ?";
        this.deleteLockSql = "DELETE FROM " + tableName
                + " WHERE lock_key = ? AND lock_owner = ?";
    }

    /**
     * 在分布式锁内执行业务逻辑。
     *
     * @param lockKey  锁 key
     * @param waitTime 获取锁最长等待时间
     * @param action   业务回调
     * @param <T>      业务返回值类型
     * @return 业务回调返回值
     */
    @Override
    public <T> T execute(String lockKey, Duration waitTime, Supplier<T> action) {
        return execute(lockKey, waitTime, DEFAULT_TIMEOUT_MESSAGE, action);
    }

    /**
     * 在分布式锁内执行业务逻辑，并自定义锁等待超时提示。
     *
     * @param lockKey        锁 key
     * @param waitTime       获取锁最长等待时间
     * @param timeoutMessage 锁等待超时提示
     * @param action         业务回调
     * @param <T>            业务返回值类型
     * @return 业务回调返回值
     */
    @Override
    public <T> T execute(String lockKey, Duration waitTime, String timeoutMessage, Supplier<T> action) {
        Objects.requireNonNull(lockKey, "lockKey must not be null");
        Objects.requireNonNull(waitTime, "waitTime must not be null");
        Objects.requireNonNull(timeoutMessage, "timeoutMessage must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (waitTime.isZero() || waitTime.isNegative()) {
            throw new IllegalArgumentException("waitTime must be positive");
        }

        String owner = LOCK_OWNERS.get();
        long lockStartTime = System.nanoTime();
        acquireWithRetry(lockKey, waitTime, timeoutMessage, owner);
        log.debug("获取分布式锁成功：provider=mysql, lockKey={}, waitMs={}",
                lockKey, elapsedMs(lockStartTime));

        ScheduledFuture<?> renewalFuture = scheduleRenewal(lockKey, owner);
        try {
            return action.get();
        } finally {
            renewalFuture.cancel(false);
            unlockQuietly(lockKey, owner);
        }
    }

    /**
     * 关闭租期续期线程池。
     */
    @Override
    public void close() {
        renewalExecutor.shutdownNow();
    }

    /**
     * 按重试间隔尝试获取锁，直到成功或超过等待时间。
     *
     * @param lockKey        锁 key
     * @param waitTime       等待时间
     * @param timeoutMessage 超时提示
     * @param owner          当前线程锁持有者
     */
    private void acquireWithRetry(String lockKey, Duration waitTime, String timeoutMessage, String owner) {
        long deadline = System.nanoTime() + waitTime.toNanos();
        while (!tryAcquire(lockKey, owner)) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                log.warn("获取分布式锁超时：provider=mysql, lockKey={}, waitTime={}", lockKey, waitTime);
                throw new BusinessException(LOCK_TIMEOUT_CODE, timeoutMessage);
            }
            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(remaining, retryInterval.toNanos()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取分布式锁被中断：provider=mysql, lockKey={}", lockKey, e);
                throw new BusinessException(ApiResponseEnum.SERVICE_UNAVAILABLE);
            }
        }
    }

    /**
     * 在事务内尝试获取锁。
     *
     * @param lockKey 锁 key
     * @param owner   当前线程锁持有者
     * @return 是否获取成功
     */
    private boolean tryAcquire(String lockKey, String owner) {
        Boolean acquired = transactionTemplate.execute(status -> {
            List<MysqlLockRow> rows = jdbcTemplate.query(selectLockSql, LOCK_ROW_MAPPER, lockKey);
            MysqlLockRow row = rows.isEmpty() ? null : rows.get(0);
            if (row == null) {
                try {
                    jdbcTemplate.update(insertLockSql, lockKey, owner, leaseMicros());
                    return true;
                } catch (DuplicateKeyException e) {
                    // 并发初始化同一锁行时，唯一键冲突表示本轮抢锁失败。
                    return false;
                }
            }
            if (row.owner().equals(owner)) {
                jdbcTemplate.update(updateReentrantSql, leaseMicros(), lockKey);
                return true;
            }
            if (row.expired()) {
                jdbcTemplate.update(updateExpiredSql, owner, leaseMicros(), lockKey);
                return true;
            }
            return false;
        });
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 安排当前锁租期自动续期。
     *
     * @param lockKey 锁 key
     * @param owner   当前线程锁持有者
     * @return 续期任务
     */
    private ScheduledFuture<?> scheduleRenewal(String lockKey, String owner) {
        long period = Math.max(1, leaseTime.toMillis() / 3);
        return renewalExecutor.scheduleAtFixedRate(() -> renewQuietly(lockKey, owner),
                period, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 静默续期，租约被抢走时只记录日志。
     *
     * @param lockKey 锁 key
     * @param owner   当前线程锁持有者
     */
    private void renewQuietly(String lockKey, String owner) {
        try {
            if (jdbcTemplate.update(renewSql, leaseMicros(), lockKey, owner) == 0) {
                log.warn("分布式锁租约已丢失：provider=mysql, lockKey={}", lockKey);
            }
        } catch (DataAccessException e) {
            log.error("分布式锁续期失败：provider=mysql, lockKey={}", lockKey, e);
        }
    }

    /**
     * 释放当前线程持有的锁，重入时仅减少计数。
     *
     * @param lockKey 锁 key
     * @param owner   当前线程锁持有者
     */
    private void unlockQuietly(String lockKey, String owner) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                List<MysqlLockRow> rows = jdbcTemplate.query(selectLockSql, LOCK_ROW_MAPPER, lockKey);
                if (rows.isEmpty() || !rows.get(0).owner().equals(owner)) {
                    log.warn("分布式锁在解锁前已丢失：provider=mysql, lockKey={}", lockKey);
                    return;
                }
                if (rows.get(0).count() > 1) {
                    jdbcTemplate.update(decreaseCountSql, lockKey, owner);
                } else {
                    jdbcTemplate.update(deleteLockSql, lockKey, owner);
                }
            });
        } catch (RuntimeException e) {
            log.error("释放分布式锁失败：provider=mysql, lockKey={}", lockKey, e);
        }
    }

    /**
     * 校验并返回安全表名。
     *
     * @param configuredName 配置表名
     * @return 安全表名
     */
    private static String safeTableName(String configuredName) {
        if (configuredName == null || !TABLE_NAME_PATTERN.matcher(configuredName).matches()) {
            throw new IllegalArgumentException("MySQL lock table name is invalid: " + configuredName);
        }
        return configuredName;
    }

    /**
     * 校验正数时长。
     *
     * @param value 配置值
     * @param name  配置名
     * @return 正数时长
     */
    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    /**
     * 租约时长换算为微秒。
     *
     * @return 微秒数
     */
    private int leaseMicros() {
        return Math.toIntExact(leaseTime.toNanos() / 1_000);
    }

    /**
     * 计算锁等待耗时。
     *
     * @param startTime 开始时间纳秒
     * @return 耗时毫秒
     */
    private static long elapsedMs(long startTime) {
        return Math.round((System.nanoTime() - startTime) / 1_000_000.0);
    }

    /**
     * MySQL 锁行数据。
     *
     * @param owner   锁持有者
     * @param count   重入次数
     * @param expired 租约是否已过期
     */
    private record MysqlLockRow(String owner, int count, boolean expired) {
    }

    /**
     * MySQL 锁行映射。
     */
    private static final RowMapper<MysqlLockRow> LOCK_ROW_MAPPER = new RowMapper<>() {

        /**
         * 读取单行锁数据。
         *
         * @param rs     结果集
         * @param rowNum 行号
         * @return 锁行数据
         * @throws SQLException 读取失败时抛出
         */
        @Override
        public MysqlLockRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MysqlLockRow(
                    rs.getString("lock_owner"),
                    rs.getInt("lock_count"),
                    rs.getBoolean("expired"));
        }
    };
}
