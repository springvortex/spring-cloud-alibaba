package com.zjc.mail.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zjc.mail.entity.MailLog;
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
                new MapperBuilderAssistant(configuration, ""), MailLog.class);
    }

    /**
     * 验证 insertFill 同时填充 createTime 和 updateTime。
     */
    @Test
    @DisplayName("insertFill: 填充 createTime 和 updateTime")
    void testInsertFillFillsBoth() {
        MailLog mailLog = new MailLog();
        MetaObject metaObject = SystemMetaObject.forObject(mailLog);

        handler.insertFill(metaObject);

        assertThat(mailLog.getCreateTime()).isNotNull().isInstanceOf(LocalDateTime.class);
        assertThat(mailLog.getUpdateTime()).isNotNull().isInstanceOf(LocalDateTime.class);
    }

    /**
     * 验证 updateFill 只填充 updateTime，不影响 createTime。
     */
    @Test
    @DisplayName("updateFill: 仅填充 updateTime")
    void testUpdateFillFillsUpdateTimeOnly() {
        MailLog mailLog = new MailLog();
        MetaObject metaObject = SystemMetaObject.forObject(mailLog);

        handler.updateFill(metaObject);

        assertThat(mailLog.getUpdateTime()).isNotNull();
        assertThat(mailLog.getCreateTime()).isNull();
    }
}
