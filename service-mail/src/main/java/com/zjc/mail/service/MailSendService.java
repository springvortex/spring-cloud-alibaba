package com.zjc.mail.service;

import com.zjc.common.dto.MailLogDTO;
import com.zjc.common.dto.MailSendDTO;

/**
 * 邮件发送服务接口。
 *
 * <p>封装邮件发送 + 记录入库的完整流程，调用方只需传入业务参数。
 *
 * @author jiancai.zhong
 */
public interface MailSendService {

    /**
     * 发送邮件并记录入库。
     *
     * @param dto 邮件发送请求（收件人、主题、正文等）
     * @return 邮件记录（含主键、发件人、发送状态）
     */
    MailLogDTO send(MailSendDTO dto);
}
