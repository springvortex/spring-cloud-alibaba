package com.zjc.provider.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MyBatis-Plus 自动填充处理器。
 *
 * <p>配合实体字段上的 {@code @TableField(fill = ...)} 注解使用，
 * 在插入/更新时自动填充时间字段，无需业务代码手动赋值：
 * <ul>
 *   <li>新增时：create_time、update_time 均设为当前时间</li>
 *   <li>更新时：仅刷新 update_time</li>
 * </ul>
 *
 * @author jiancai.zhong
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * 业务时区，所有审计时间字段统一使用东八区
     */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 新增时填充：create_time 和 update_time 都设为当前时间。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now(ZONE));
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now(ZONE));
    }

    /**
     * 更新时填充：仅刷新 update_time。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now(ZONE));
    }
}
