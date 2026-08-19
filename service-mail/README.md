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

下表是 Controller 资源路径，实际请求路径自动追加 `/api/v1/mail` 前缀。

| 方法 | 路径    | 说明                         |
|------|---------|------------------------------|
| POST | `/send` | 发送纯文本或 HTML 邮件并入库 |

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

版本分组由 `service-common` 自动生成：

| 分组      | 路径匹配          |
|-----------|-------------------|
| `v1-mail` | `/api/v1/mail/**` |

## 包结构

```
com.zjc.mail
├── MailApplication              启动类
├── config
│   ├── AuditMetaObjectHandler   自动填充 createTime / updateTime
│   ├── MybatisPlusConfig        分页插件
│   └── OpenApiConfig            SpringDoc 元信息配置
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

配置来源由 `app.env` 与 `app.config.source` 组合控制，默认值为 `dev,local`。

- **local**：加载 `src/main/resources/config/application-dev.yaml`，其中维护 SMTP、数据源和 MyBatis-Plus 配置。
- **remote**：通过 `src/main/resources/config/application-remote.yaml` 拉取 Nacos，dataId 为 `${zjc.config.env}`，
  group 为 `service-mail`，namespace 为 `public`。

Nacos 地址统一来自 `${zjc.infrastructure.host}:8848`，可通过 `INFRASTRUCTURE_HOST` 覆盖。

remote 模式的 Nacos 配置中至少需要维护：

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

## 日志与链路追踪

本模块通过 `service-config` 统一日志配置，日志输出到：

```text
logs/service-mail/
```

日志包含 `traceId` 和 `spanId`。模块同时引入 Actuator 与 Zipkin，可追踪 Gateway 到 Mail 的调用耗时，也可用于定位 SMTP 发送异常：

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
    export:
      zipkin:
        enabled: ${ZIPKIN_EXPORT_ENABLED:true}
        endpoint: "${ZIPKIN_ENDPOINT:http://${zjc.infrastructure.host}:9411/api/v2/spans}"
      enabled: ${TRACING_ENABLED:true}
```

## 依赖

- service-common
- service-config
- spring-cloud-starter-alibaba-nacos-discovery / config
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-zipkin
- spring-boot-starter-mail
- mybatis-plus-spring-boot4-starter
- mybatis-plus-jsqlparser
- mysql-connector-j
- mapstruct
- springdoc-openapi-starter-webmvc-ui
