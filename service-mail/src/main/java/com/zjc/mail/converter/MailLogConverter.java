package com.zjc.mail.converter;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.mail.entity.MailLog;
import org.mapstruct.Mapper;

/**
 * 邮件记录 Entity <-> DTO 转换器。
 *
 * <p>MapStruct 在编译期生成实现类，零反射。
 *
 * @author jiancai.zhong
 */
@Mapper(componentModel = "spring")
public interface MailLogConverter {

    /**
     * 邮件记录 Entity 转 DTO。
     *
     * @param mailLog 邮件记录实体，为 {@code null} 时返回 {@code null}
     * @return 邮件记录 DTO
     */
    MailLogDTO entityToDto(MailLog mailLog);
}
