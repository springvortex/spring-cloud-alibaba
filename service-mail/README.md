# Service Mail

统一邮件服务，提供收发邮件能力，其他模块只需通过 Feign 调用即可。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9004                                  |
| 服务名     | service-mail                          |
| Swagger UI | http://localhost:9004/swagger-ui.html |

## 接口

### 邮件发送

| 方法 | 路径         | 说明                                   |
|------|--------------|----------------------------------------|
| POST | `/mail/send` | 发送邮件，返回发送记录（含主键和状态） |

#### 请求参数（MailSendDTO）

| 字段      | 类型    | 必填 | 说明                       |
|-----------|---------|------|----------------------------|
| toEmails  | String  | 是   | 收件人，多个用英文逗号分隔 |
| ccEmails  | String  | 否   | 抄送人，多个用英文逗号分隔 |
| bccEmails | String  | 否   | 密送人，多个用英文逗号分隔 |
| subject   | String  | 是   | 邮件主题                   |
| content   | String  | 是   | 邮件正文                   |
| isHtml    | Boolean | 否   | 是否 HTML 格式，默认 false |

发件人和 SMTP 配置由邮件模块统一管理，调用方无需关心。

#### 其他模块调用方式

引入 service-common 依赖后，直接注入 `MailFeignApi`：

```java
@Resource
private MailFeignApi mailFeignApi;

public void sendMail() {
    MailSendDTO dto = new MailSendDTO();
    dto.setToEmails("user@example.com");
    dto.setSubject("验证码通知");
    dto.setContent("您的验证码是 123456，5分钟内有效。");
    ApiResponse<MailLogDTO> resp = mailFeignApi.send(dto);
}
```

### 系统信息

| 方法 | 路径           | 说明                         |
|------|----------------|------------------------------|
| GET  | `/system/info` | 查询项目构建元数据与运行环境 |

## SpringDoc 分组

| 分组           | 路径匹配        |
|----------------|-----------------|
| 01-邮件服务    | `/mail/**`      |
| 02-系统信息    | `/system/**`    |

## 发送流程

1. 校验收件人/抄送/密送邮箱格式，非法地址直接拦截（不写库不发送）
2. 入库一条待发送记录（status=0）
3. 调用 SMTP 发送邮件
4. 成功更新 status=1，失败更新 status=2 并记录错误信息

## 数据表

`t_mail_log` 邮件发送记录表：

| 字段        | 类型     | 说明                          |
|-------------|----------|-------------------------------|
| mail_id     | bigint   | 主键，雪花算法生成            |
| from_email  | varchar  | 发件人                        |
| to_emails   | varchar  | 收件人（逗号分隔）            |
| cc_emails   | varchar  | 抄送人                        |
| bcc_emails  | varchar  | 密送人                        |
| subject     | varchar  | 邮件主题                      |
| content     | text     | 邮件正文                      |
| is_html     | tinyint  | 是否 HTML（1是 0否）          |
| status      | tinyint  | 发送状态（0待发 1成功 2失败） |
| error_msg   | varchar  | 失败原因                      |
| is_deleted  | tinyint  | 逻辑删除                      |
| create_time | datetime | 创建时间                      |
| update_time | datetime | 更新时间                      |

## 包结构

```
com.zjc.mail
├── MailApplication                 启动类
├── config
│   ├── AuditMetaObjectHandler      自动填充 createTime / updateTime
│   ├── MybatisPlusConfig           分页插件
│   └── OpenApiConfig               SpringDoc 分组配置
├── controller
│   ├── MailController              邮件发送 REST 接口
│   └── SystemInfoController        系统信息接口
├── entity
│   └── MailLog                     邮件记录实体（映射 t_mail_log）
├── mapper
│   └── MailLogMapper               MyBatis-Plus Mapper
└── service / impl
    ├── MailSendService             发送业务
    ├── MailLogService              记录管理
    └── impl
        ├── MailSendServiceImpl     发送实现（校验、入库、发送、状态更新）
        └── MailLogServiceImpl
```

## 配置说明

Nacos 配置位置：dataId=`dev`，group=`service-mail`

SMTP 相关配置在 Nacos 中：

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: noreply@example.com
    password: 授权码
    properties:
      mail:
        smtp:
          ssl:
            enable: true
          auth: true
          starttls:
            enable: true
```

## 构建信息

pom.xml 配置了 `spring-boot-maven-plugin` 的 `build-info` 目标，编译期生成 `META-INF/build-info.properties`，
供 `/system/info` 接口读取项目名称、版本、构建时间等元数据。

## 依赖

- service-common
- spring-boot-starter-web / actuator
- spring-boot-starter-mail
- mybatis-plus-spring-boot4-starter
- mysql-connector-j
- springdoc-openapi-starter-webmvc-ui
