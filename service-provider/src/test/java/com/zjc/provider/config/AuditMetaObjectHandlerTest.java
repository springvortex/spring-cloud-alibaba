package com.zjc.provider.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zjc.provider.entity.Goods;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AuditMetaObjectHandler} 单元测试。
 *
 * <p>验证 insert/update 时自动填充 createTime、updateTime 的逻辑。
 * 因 strictInsertFill/strictUpdateFill 内部依赖 TableInfo 注册表，
 * 在 @BeforeAll 中手动初始化 Goods 的表信息。
 *
 * @author jiancai.zhong
 */
@DisplayName("MyBatis-Plus 自动填充处理器")
class AuditMetaObjectHandlerTest {

    private final AuditMetaObjectHandler handler = new AuditMetaObjectHandler();

    @BeforeAll
    static void initTableInfo() {
        Configuration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, ""), Goods.class);
    }

    /**
     * 验证 insertFill 同时填充 createTime 和 updateTime 两个字段。
     */
    @Test
    @DisplayName("insertFill: 填充 createTime 和 updateTime")
    void testInsertFillFillsBoth() {
        Goods goods = new Goods();
        MetaObject metaObject = SystemMetaObject.forObject(goods);

        handler.insertFill(metaObject);

        assertThat(goods.getCreateTime()).isNotNull().isInstanceOf(LocalDateTime.class);
        assertThat(goods.getUpdateTime()).isNotNull().isInstanceOf(LocalDateTime.class);
    }

    /**
     * 验证 insertFill 不会覆盖已有的 createTime（strictInsertFill 的保留逻辑）。
     */
    @Test
    @DisplayName("insertFill: 已有值时不覆盖 createTime")
    void testInsertFillKeepsExisting() {
        Goods goods = new Goods();
        LocalDateTime fixed = LocalDateTime.of(2020, 1, 1, 0, 0);
        goods.setCreateTime(fixed);
        MetaObject metaObject = SystemMetaObject.forObject(goods);

        handler.insertFill(metaObject);

        assertThat(goods.getCreateTime()).isEqualTo(fixed);
        assertThat(goods.getUpdateTime()).isNotNull();
    }

    /**
     * 验证 updateFill 只填充 updateTime，不影响 createTime。
     */
    @Test
    @DisplayName("updateFill: 仅填充 updateTime")
    void testUpdateFillFillsUpdateTimeOnly() {
        Goods goods = new Goods();
        MetaObject metaObject = SystemMetaObject.forObject(goods);

        handler.updateFill(metaObject);

        assertThat(goods.getUpdateTime()).isNotNull();
        assertThat(goods.getCreateTime()).isNull();
    }
}
