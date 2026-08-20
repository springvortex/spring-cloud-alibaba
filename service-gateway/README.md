# Service Gateway

API 网关，基于 Spring Cloud Gateway（WebFlux），统一入口与路由。

## 基本信息

| 项     | 值              |
|--------|-----------------|
| 端口   | 80              |
| 服务名 | service-gateway |

> **注意**：Linux 上非 root 用户不能绑定 80 端口，生产环境需改为 1024 以上端口（如 9000）。

## 职责

- 统一路由入口，将请求分发到下游各服务
- 维护 local 路由基线，并在 remote 模式通过 Nacos 维护动态路由
- 统一配置 CORS 跨域策略
- 负载均衡（LoadBalancer + Caffeine 缓存）
- 输出请求开始、结束、耗时与结束信号日志
- 传播 W3C `traceparent`，保证全链路使用同一个 `traceId`

## 包结构

```
com.zjc.gateway
├── GatewayApplication              启动类（@EnableDiscoveryClient）
├── config
│   └── ObservabilityConfiguration  开启 Reactor 自动上下文传播
├── exception
│   └── GatewayErrorWebExceptionHandler 统一 JSON 错误响应
└── filter
    └── ServiceGlobalFilter         全局请求日志过滤器
```

## 路由配置

local 模式下路由规则位于 `src/main/resources/config/application-dev.yaml`，作为本地开发基线。remote 模式通过
`src/main/resources/config/application-remote.yaml` 拉取 Nacos，dataId 固定为 `prod`，group 为 `service-gateway`，
namespace 为 `public`。

当前业务服务统一使用 `/api/{版本}/{模块}` 路径。由于下游服务本身已经接收完整前缀， 网关只按模块转发，不做 `RewritePath`
剥离前缀：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: provider-route
              uri: lb://service-provider
              predicates:
                - Path=/api/*/provider/**
              order: 1
            - id: consumer-route
              uri: lb://service-consumer
              predicates:
                - Path=/api/*/consumer/**
              order: 2
            - id: mail-route
              uri: lb://service-mail
              predicates:
                - Path=/api/*/mail/**
              order: 3
```

路径中的 `*` 表示版本号，当前 v1 和后续 v2 都可以原样转发，例如：
`/api/v1/provider/user/1` 会转发到 `service-provider` 的同一路径。

### 跨域配置

CORS 与路由相同：local 模式使用 `config/application-dev.yaml` 中的基线，remote 模式使用 Nacos 中对应环境的配置：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          globalcors:
            cors-configurations:
              '[/**]':
                allowed-origin-patterns:
                  - '*'
                allowed-headers:
                  - '*'
                allowed-methods:
                  - '*'
                allow-credentials: false
                max-age: 3600
```

配置说明：

- `allowed-origin-patterns`：允许的来源，当前开发环境使用通配符。
- `allowed-methods` / `allowed-headers`：允许全部常用方法和请求头。
- `allow-credentials: false`：不允许携带 Cookie 等凭证；如果改为 `true`，来源不能继续使用 `*`，必须配置明确域名。
- `max-age: 3600`：浏览器对预检请求结果缓存 1 小时，减少 `OPTIONS` 请求。

路由和 CORS 都属于会随环境变化的运行配置。当前没有写在 Java 配置类中：local 模式保留 YAML 基线便于离线开发，
remote 模式交给 Nacos 按环境调整，并利用配置动态刷新能力，不需要重新打包网关代码。

### 请求示例

```bash
curl "http://127.0.0.1/api/v1/provider/user/1"
curl "http://127.0.0.1/api/v1/consumer/feign/port"
curl -X POST "http://127.0.0.1/api/v1/mail/send" \
  -H "Content-Type: application/json" \
  -d '{"toEmails":"a@example.com","subject":"测试","content":"Hello","isHtml":false}'
```

生产环境将 `127.0.0.1` 替换为 Gateway 对外域名或 IP，只暴露 Gateway 端口。

## 网关过滤器

全局请求日志过滤器：

```text
com.zjc.gateway.filter.ServiceGlobalFilter
```

它实现 `GlobalFilter` 和 `Ordered`：

- 前置逻辑记录 HTTP 方法和完整 URI。
- `chain.filter(exchange)` 放行请求到后续过滤器和目标服务。
- `doFinally` 在请求完成、异常或取消时记录结束时间、耗时和 `SignalType`。
- `getOrder()` 返回 `0`，后续如需增加鉴权、签名、灰度过滤器，可通过 order 控制执行顺序。

核心实现：

```java
long startTime = System.currentTimeMillis();
log.info("开始请求{} {}", method, uri);

return chain.filter(exchange)
        .doFinally(signalType -> {
            long endTime = System.currentTimeMillis();
            log.info("结束请求 {} {}，耗时：{}ms，信号：{}",
                    method, uri, endTime - startTime, signalType);
        });
```

使用 `doFinally` 而不是 `doOnSuccess` 的原因是：它同时覆盖正常完成、错误终止和取消，网关日志不会因为下游异常而缺失结束记录。

## 统一错误响应

Gateway 基于 WebFlux，不能复用业务服务的 WebMVC `GlobalExceptionHandler`。网关使用：

```text
com.zjc.gateway.exception.GatewayErrorWebExceptionHandler
```

它实现 Spring Boot 的 `ErrorWebExceptionHandler`，优先级高于默认 Whitelabel Error Page。当访问不存在的路由（如 `/`）、
下游服务不可用或网关处理链抛出其他异常时，会返回与业务服务一致的 JSON 结构：

```json
{
  "success": false,
  "code": 102,
  "message": "请求路径不存在，请检查接口地址或网关路由",
  "data": null,
  "timestamp": 1787191865837
}
```

错误处理策略：

- HTTP 404 映射业务码 `102`，提示请求路径或路由不存在。
- HTTP 503 映射业务码 `503`，用于下游服务不存在、不可用或负载均衡找不到实例。
- 其他 HTTP 状态码按 `ApiResponseEnum` 的码段规则映射，未识别状态使用通用失败码 `-1`。
- 4xx 记录 warn 日志，5xx 记录 error 日志；响应体只输出稳定提示，不暴露内部异常细节。
- 不会拦截已经成功转发的业务响应；业务服务返回的 JSON 会原样透传。

## 入口与端口边界

生产环境只把 Gateway 暴露给公网，其他服务组件均仅内网访问：

| 服务 / 组件      | 端口                        | 公网暴露 |
|------------------|-----------------------------|----------|
| service-gateway  | 80 / 443（或生产改用 9000） | 允许     |
| service-provider | 9001                        | 禁止     |
| service-consumer | 9002                        | 禁止     |
| service-mail     | 9004                        | 禁止     |
| Nacos            | 8848 / 9848 / 9849 / 7848   | 禁止     |
| MySQL            | 3306                        | 禁止     |
| Zipkin           | 9411                        | 禁止     |

访问链路固定为：

```text
公网客户端 -> service-gateway -> lb://service-provider / service-consumer / service-mail
```

不要在安全组、防火墙或 Docker 端口映射中开放业务服务、Nacos、MySQL 或 Zipkin 端口。业务服务的独立地址和 Swagger UI
只用于本机或内网调试，生产环境统一通过 Gateway 访问 `/api/{版本}/{模块}/**`。

## 配置说明

`application.yaml` 保留端口、服务名和公共 profile include；local 模式下路由和 CORS 从 `config/application-dev.yaml` 加载。
remote 模式下 Nacos dataId 固定为 `prod`，group 为 `service-gateway`。Nacos 地址统一来自
`${zjc.infrastructure.host}:8848`，可通过 `INFRASTRUCTURE_HOST` 覆盖。

本地 `dev,local` 组合通过 `/swagger-ui.html` 聚合 Provider、Consumer、Mail 的 OpenAPI 文档；生产
`prod,remote` 组合保持公共配置中的 SpringDoc 默认关闭状态，且网关不注册 OpenAPI 转发路由。

链路追踪配置来自 `service-config` 的 `zipkin` profile，由 `spring.profiles.include` 激活：

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

Gateway 是 WebFlux 异步链路，`ObservabilityConfiguration` 启动时调用：

```java
Hooks.enableAutomaticContextPropagation();
```

这样 Micrometer Tracing 能把 Reactor Context 中的追踪上下文恢复到回调线程的 MDC，`ServiceGlobalFilter` 的开始/结束日志才能稳定输出同一个
`traceId` 和 `spanId`。

## 设计说明

网关作为纯路由转发层，保持轻量，除本地 OpenAPI 聚合入口外不集成以下功能：

- 不依赖 service-common（避免 WebMVC 与 WebFlux 冲突）
- 不为业务接口生成 OpenAPI 文档
- 不包含测试模块
- 不提供业务接口
- 不自动继承 GlobalExceptionHandler 和 WebLogAspect（WebFlux 不支持 `@RestControllerAdvice`）

> **注意**：Gateway 的请求日志已由 `ServiceGlobalFilter` 提供；如需统一异常处理，应编写 WebFlux 版
> `ErrorWebExceptionHandler`，不能复用 WebMVC 的 `GlobalExceptionHandler`。

## 依赖

- spring-cloud-starter-gateway-server-webflux
- service-config
- spring-boot-starter-actuator
- spring-boot-starter-zipkin
- springdoc-openapi-starter-webflux-ui（仅本地聚合 Swagger UI）
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- spring-cloud-starter-alibaba-nacos-discovery / config

## WebFlux 约束

Gateway 基于 WebFlux，不能与 `spring-boot-starter-web`（WebMVC）共存。引入 service-common 会带来 WebMVC 依赖，因此 Gateway
不依赖 common。
