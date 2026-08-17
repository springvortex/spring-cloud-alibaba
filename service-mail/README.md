# Service Mail

邮件服务，统一发送纯文本和 HTML 邮件，并把发送过程记录到 `t_mail_log`。其他微服务可以通过 common 模块的
`MailFeignApi` 或网关直接调用。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9004                                  |
| 服务名     | service-mail                          |
| Swagger UI | http://localhost:9004/swagger-ui.html |

## 接口

| 方法 | 路径         | 说明                         |
|------|--------------|------------------------------|
| POST | `/mail/send` | 发送纯文本或 HTML 邮件并入库 |

请求示例：

```json
{
  "toEmails": "a@example.com,b@example.com",
  "ccEmails": "c@example.com",
  "bccEmails": "d@example.com",
  "subject": "测试邮件",
  "content": "<p>Hello Service Mail</p>",
  "isHtml": true
}
```

`toEmails`、`subject`、`content` 必填；抄送和密送可选。多个邮箱用英文逗号分隔。发件人来自 Nacos 中的
`spring.mail.username`，调用方不能指定。

发送记录状态：

| 状态 | 含义     |
|------|----------|
| 0    | 待发送   |
| 1    | 发送成功 |
| 2    | 发送失败 |

SMTP 异常不会抛给调用方，而是写入 `errorMsg` 并返回 `status=2` 的记录；调用方需要检查 `data.status` 判断实际发送结果。

## SpringDoc 分组

| 分组        | 路径匹配   |
|-------------|------------|
| 01-邮件服务 | `/mail/**` |

## 包结构

```
com.zjc.mail
├── MailApplication              启动类
├── config
│   ├── AuditMetaObjectHandler   自动填充 createTime / updateTime
│   ├── MybatisPlusConfig        分页插件
│   └── OpenApiConfig            SpringDoc 配置
├── controller                   MailController
├── converter                    MapStruct Entity/DTO 转换器
├── entity                       MailLog，映射 t_mail_log
├── mapper                       MailLogMapper
└── service / impl              邮件发送与发送记录服务
```

## 发送流程

1. 校验收件人、抄送人和密送人的邮箱格式。
2. 写入一条 `status=0` 的待发送记录。
3. 根据 `isHtml` 选择 `SimpleMailMessage` 或 `MimeMessageHelper` 发送。
4. 发送成功更新为 `status=1`；异常时更新为 `status=2` 并记录 `errorMsg`。
5. 通过 MapStruct 将实体转换为 `MailLogDTO` 返回。

## 配置说明

本地 `application.yaml` 只保留端口、profile 和 Nacos 引导配置。业务配置在 Nacos：

- dataId：`dev`
- group：`service-mail`
- 当前引导配置地址：`192.168.100.128:8848`

Nacos 中至少需要配置：

```yaml
spring:
  mail:
    host: your-smtp-host
    username: your-from-email
    password: ENC(your-encrypted-password)
    port: 465
    properties:
      mail.smtp.auth: true
      mail.smtp.ssl.enable: true
```

SMTP 密码使用 Jasypt 密文，启动时通过 `jasypt.encryptor.password` 或 `JASYPT_ENCRYPTOR_PASSWORD` 注入密钥。

## 依赖

- service-common
- spring-cloud-starter-alibaba-nacos-discovery / config
- spring-boot-starter-web
- spring-boot-starter-mail
- mybatis-plus-spring-boot4-starter
- mybatis-plus-jsqlparser
- mysql-connector-j
- mapstruct
- springdoc-openapi-starter-webmvc-ui
