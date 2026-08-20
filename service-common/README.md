# Service Common

公共模块，业务服务的基础依赖（Gateway 基于 WebFlux，刻意不引入）。引入此模块后自动获得全局异常处理、接口日志切面、配置加密等能力。

## 职责

- 统一响应封装 `ApiResponse`
- 全局异常处理 `GlobalExceptionHandler`（自动注册，业务模块无需配置）
- 接口日志切面 `WebLogAspect`（自动注册，业务模块无需配置）
- 业务异常 `BusinessException` + 错误码体系 `ErrorCode`
- 跨服务传输 DTO
- 跨服务共享的 Feign API 契约
- 配置加密（Jasypt，引入后自动生效）
- OpenFeign / SpringDoc / Web MVC 相关的公共编译 API

## 包结构

```
com.zjc.common
├── aop
│   └── WebLogAspect               Web 接口日志切面（入参/返回/耗时）
├── api
│   ├── mail                       MailFeignApi - 邮件服务 Feign 客户端
│   ├── test                       TestApi - 连通性测试 Feign 客户端
│   └── user
│       ├── UserFeignApi           用户服务 Feign 客户端
│       └── factory                UserFeignFallbackFactory - 用户接口降级工厂
├── constant
│   ├── ApiResponseEnum            响应码标准枚举（实现 ErrorCode 接口）
│   └── ErrorCode                  错误码接口
├── dto                            跨服务传输对象
│   ├── UserDTO / GoodsDTO / OrderDTO / OrderDetailDTO
│   ├── MailSendDTO / MailLogDTO
├── exception
│   ├── BusinessException          业务异常（供 Service / Controller 抛出）
│   └── GlobalExceptionHandler     全局异常处理器（@RestControllerAdvice）
└── web
    ├── annotation                 ApiVersion - Controller 版本标注
    ├── ApiPathAutoConfiguration   API 前缀、SpringDoc 分组与 Feign 拦截器自动装配
    ├── ApiPathProperties          zjc.api 配置属性
    ├── ApiPathResolver            服务名 / 版本到 API 前缀的解析逻辑
    └── ApiResponse                统一响应封装
```

## 自动注册机制

common 模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册以下组件：

```
com.zjc.common.exception.GlobalExceptionHandler
com.zjc.common.web.ApiPathAutoConfiguration
com.zjc.common.aop.WebLogAspect
com.zjc.common.api.user.factory.UserFeignFallbackFactory
```

任何引入 `service-common` 依赖的 Spring Boot 应用都会自动获得全局异常处理、统一 API 路径能力、SpringDoc 版本分组、
Feign 前缀拦截器和用户 Feign 降级工厂，无需手动
`@Import` 或
`@ComponentScan`。

> **注意**：Gateway 基于 WebFlux，`@RestControllerAdvice` 和 `@RestController` 切面对它无效。Gateway 需要单独编写 WebFlux
> 版本。

## 统一响应

所有接口统一返回 `ApiResponse<T>`，结构：

```json
{
  "success": true,
  "code": 0,
  "message": "操作成功",
  "data": {},
  "timestamp": 1723017600000
}
```

响应码段位规划：

| 码段 | 含义         |
|------|--------------|
| 0    | 成功         |
| -1   | 通用失败     |
| 1xx  | 参数校验类   |
| 4xx  | 认证授权类   |
| 5xx  | 服务端异常类 |

## 全局异常处理

`GlobalExceptionHandler` 统一拦截各层异常并用 `ApiResponse` 包装返回，业务模块无需自行编写异常处理器。

处理的异常类型：

| 异常类型                                  | 错误码 | 说明                                   |
|-------------------------------------------|--------|----------------------------------------|
| `BusinessException`                       | 透传   | 业务异常，透传自身的 code 和 message   |
| `MethodArgumentNotValidException`         | 100    | @RequestBody 校验失败                  |
| `BindException`                           | 100    | 表单参数校验失败                       |
| `ConstraintViolationException`            | 100    | @RequestParam / @PathVariable 校验失败 |
| `MissingServletRequestParameterException` | 100    | 缺少必填参数                           |
| `HttpMessageNotReadableException`         | 101    | 请求体无法解析（透传 e.getMessage()）  |
| `NoResourceFoundException`                | 102    | 请求路径不存在（favicon.ico 不记日志） |
| `HttpRequestMethodNotSupportedException`  | -1     | 请求方法不支持                         |
| `Exception`（兜底）                       | 500    | 未预期异常（透传 e.getMessage()）      |

兜底异常和 JSON 解析异常在 `e.getMessage()` 非空时透传具体原因（如 `/ by zero`），为空时回退到默认提示，避免前端拿到 null。

## 业务异常

`BusinessException` 供 Service / Controller 层抛出，配合 `ErrorCode` 体系使用：

```java
// 使用预定义错误码枚举（推荐）
throw new BusinessException(ApiResponseEnum.USER_NOT_FOUND);

// 自定义提示信息，错误码默认 -1
throw new BusinessException("用户不存在");

// 自定义错误码 + 提示信息
throw new BusinessException(10001, "用户不存在");
```

自定义错误码只需实现 `ErrorCode` 接口：

```java
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(20001, "用户不存在"),
    USER_DISABLED(20002, "用户已被禁用");

    // ...
}
```

## 接口日志切面

`WebLogAspect` 拦截所有 `@RestController` 类的公共方法，自动记录：

- 请求前：HTTP 方法、URI、类名、方法名、入参（JSON 序列化）
- 正常返回：耗时、返回值（超 2000 字符截断）
- 异常抛出：耗时、异常消息（异常继续向上抛出，由全局异常处理器接管）

跳过 `HttpServletRequest`、`HttpServletResponse`、`MultipartFile` 等不适合序列化的对象。

## 配置加密（Jasypt）

common 模块集成 `jasypt-spring-boot-starter`，所有引入 common 的模块自动支持配置加密。

### 加密明文

运行 `com.zjc.common.JasyptTest`，在 VM Options 中填入密钥 `-Djasypt.encryptor.password=your-secret-key`，输入明文即可得到
`ENC(xxx)` 密文。

### 使用方式

在 Nacos 配置中用 `ENC(xxx)` 替换明文：

```yaml
spring:
  datasource:
    password: ENC(g48ZFqzM2yvuAMjOMw7z77DB7jTw9JjTkcJcuvo+Zkc=)
```

启动服务时传入密钥（不写入配置文件）：

```bash
# JVM 系统属性
java -Djasypt.encryptor.password=your-secret-key -jar service-provider-1.0.0.jar

# 环境变量
export JASYPT_ENCRYPTOR_PASSWORD=your-secret-key
java -jar service-provider-1.0.0.jar
```

> **安全注意**：密钥禁止写死在配置文件中，仅通过命令行参数或环境变量注入。

### 加密算法配置

Jasypt 加密参数统一来自 `service-config` 的 `config/application-jasypt.yaml`。算法为 `PBEWithMD5AndDES`，与 `JasyptTest`
工具完全一致。
当前使用已适配 Spring Boot 4.x 的
jasypt-spring-boot-starter 4.0.4；仍显式声明这些参数，避免依赖隐式默认值，并便于集中管理。

> **IDEA 本地开发**：在 Run Configuration -> VM Options 中填入 `-Djasypt.encryptor.password=your-secret-key`
> 。如果通过系统环境变量传入，需彻底退出 IDEA 再重新打开才能继承。

## 统一 API 路径

业务 Controller 只编写资源路径，本模块根据 `spring.application.name` 自动生成
`/api/{版本}/{模块}` 前缀，例如 `service-provider` 使用 `/api/v1/provider/**`。

```yaml
zjc:
  api:
    prefix: /api
    versions:
      - v1
```

当前配置只启用 `v1`，未标注 `@ApiVersion` 的 Controller 使用版本列表中的第一个版本。如需 v1/v2 共存，可在
`zjc.api.versions` 中追加 `v2`，并显式配置 `default-version`；v2 Controller 标注 `@ApiVersion("v2")`。 SpringDoc 分组由本模块自动生成，Feign
调用也会在发送前追加目标服务的前缀。

## 共享 Feign API

common 模块中定义了跨服务共享的 Feign 客户端接口，其他服务引入 common 依赖后可直接注入使用。

| 接口           | 目标服务         | 契约资源路径     | 实际请求路径                     | 说明                                   |
|----------------|------------------|------------------|----------------------------------|----------------------------------------|
| `MailFeignApi` | service-mail     | `POST /send`     | `POST /api/v1/mail/send`         | 发送邮件                               |
| `TestApi`      | service-provider | `GET /port`      | `GET /api/v1/provider/port`      | 获取 provider 实例端口，验证链路连通性 |
| `UserFeignApi` | service-provider | `GET /user/{id}` | `GET /api/v1/provider/user/{id}` | 远程查询单个用户，失败时返回空 data    |
| `UserFeignApi` | service-provider | `GET /user/list` | `GET /api/v1/provider/user/list` | 远程查询用户列表，失败时返回空列表     |

## 依赖说明

该模块不打包为可执行 Spring Boot 应用（`spring-boot.repackage.skip=true`），仅作为 jar 供其他模块引入。模块只显式声明源码直接使用的
Spring MVC、Spring Boot AutoConfigure、AspectJ、OpenFeign、SpringDoc common、Jasypt、Hutool 和 Swagger 注解等 API；Servlet API
声明为 `provided`，由目标服务的 Web 运行时提供。源码通过 `maven-source-plugin` 一并打包，方便其他模块引用时查看源码。

`service-common` 不再替业务服务决定运行时技术栈：

| 运行能力 | 声明位置 |
|----------|----------|
| Web 容器 / Spring MVC | `service-provider`、`service-consumer`、`service-mail` |
| Swagger UI | `service-provider`、`service-consumer`、`service-mail`、`service-gateway` |
| OpenFeign starter | `service-consumer` |
| Sentinel Feign 熔断 | `service-consumer`（当前只有它启用 `feign.sentinel.enabled=true`） |
| Nacos / 配置中心 / 数据库 / 邮件 | 对应业务模块 |

这样公共库不会把 Tomcat、Swagger UI、Sentinel 等完整 starter 传递给所有下游模块，后续升级 Spring Boot / Spring Cloud
时影响面更清晰。新增公共代码如果直接 import 了新的第三方包，应同步在 `service-common/pom.xml` 显式声明，而不是继续依赖传递依赖。
