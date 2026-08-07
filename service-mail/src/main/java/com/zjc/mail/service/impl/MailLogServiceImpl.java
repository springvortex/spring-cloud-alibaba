package com.zjc.mail.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zjc.mail.entity.MailLog;
import com.zjc.mail.mapper.MailLogMapper;
import com.zjc.mail.service.MailLogService;
import org.springframework.stereotype.Service;

/**
 * 邮件记录表服务实现。
 *
 * <p>继承 {@link ServiceImpl}，泛型指定 Mapper 与实体，
 * 由 MyBatis-Plus 自动装配单表 CRUD 实现。
 *
 * @author jiancai.zhong
 */
@Service
public class MailLogServiceImpl extends ServiceImpl<MailLogMapper, MailLog> implements MailLogService {

}
