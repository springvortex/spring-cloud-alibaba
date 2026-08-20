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
- Sentinel 接口限流、单 IP 限流与路由熔断
- 输出请求开始、结束、耗时与结束信号日志
- 传播 W3C `traceparent`，保证全链路使用同一个 `traceId`

## 包结构

```
com.zjc.gateway
├── GatewayApplication              启动类（@EnableDiscoveryClient）
├── config
│   ├── ObservabilityConfiguration  开启 Reactor 自动上下文传播
│   ├── SentinelGatewayConfiguration  Sentinel Gateway 规则与熔断装配
│   ├── SentinelGatewayProperties    Sentinel 自定义配置
│   └── SentinelGatewayCircuitBreakerFilterFactory  Sentinel 路由熔断过滤器
├── exception
│   ├── GatewayErrorWebExceptionHandler  统一 JSON 错误响应
│   └── SentinelGatewayBlockRequestHandler  限流统一 JSON 响应
├── controller
│   └── GatewayFallbackController   下游故障兜底响应
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

### Sentinel 防护

网关直接集成 Sentinel，当前提供三类能力：

- 按接口维度限流：使用 Sentinel API 分组和正则路径匹配，一条规则代表一个接口或一组同构接口。
- 按 IP 限流：在接口规则上增加 `per-ip-qps`，同一客户端 IP 超过阈值后返回 429。
- 路由熔断：每条业务路由独立统计异常比例和慢请求比例，下游故障时快速转发到网关兜底响应。

Sentinel 公共配置位于 `src/main/resources/config/application-sentinel.yaml`，并在
`src/main/resources/application.yaml` 中通过 profile include 加载，本地和生产共用：

```yaml
spring:
  profiles:
    include:
      - zipkin
      - sentinel
```

`application-sentinel.yaml` 内容如下：

```yaml
spring:
  cloud:
    sentinel:
      filter:
        enabled: false
    gateway:
      server:
        webflux:
          default-filters:
            - name: CircuitBreaker
              args:
                statusCodes:
                  - INTERNAL_SERVER_ERROR
                  - BAD_GATEWAY
                  - SERVICE_UNAVAILABLE
                  - GATEWAY_TIMEOUT

zjc:
  gateway:
    sentinel:
      enabled: true
      trusted-proxies:
        - 127.0.0.1
        - ::1
        - 0:0:0:0:0:0:0:1
      interfaces:
        - name: provider-user-detail
          pattern: /api/[^/]+/provider/user/\d+
          total-qps: 100
          per-ip-qps: 10
          interval-sec: 1
        - name: mail-send
          pattern: /api/[^/]+/mail/send
          total-qps: 20
          per-ip-qps: 2
          interval-sec: 1
      circuit:
        min-request-amount: 5
        exception-ratio: 0.5
        slow-request-rt-threshold-ms: 2000
        slow-request-ratio: 0.8
        stat-interval-ms: 60000
        recovery-seconds: 10
```

`spring.cloud.sentinel.filter.enabled=false` 会关闭普通 WebFlux Sentinel 过滤器。Gateway 使用官方
`SentinelGatewayFilter`，如果两者同时开启，同一个请求会被统计两次，阈值表现也会偏离配置。

接口规则说明：

- `name`：Sentinel API 分组名，全局唯一。
- `pattern`：接口路径正则，匹配 Gateway 收到的原始路径，不区分 HTTP Method。
- `total-qps`：该接口的全局 QPS 上限。
- `per-ip-qps`：单个客户端 IP 的 QPS 上限，不能大于 `total-qps`。
- `interval-sec`：流控统计窗口。

每条接口规则会生成两条 Sentinel Gateway Flow Rule：一条全局 QPS，一条按 `CLIENT_IP` 参数限流。
被限流时返回 HTTP 429：

```json
{
  "success": false,
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": null,
  "timestamp": 1787191865837
}
```

`trusted-proxies` 表示可信的直连代理地址。只有请求直连地址在这个列表内，网关才采信
`X-Forwarded-For` 的第一个 IP；否则使用 TCP 直连地址作为客户端 IP。这样客户端直连网关时不能伪造
`X-Forwarded-For` 绕过单 IP 限流。如果生产环境前面有 Nginx、云负载均衡或 WAF，必须把它们的内网直连地址加入
`trusted-proxies`。

路由熔断使用 Gateway 的 `default-filters` 统一挂载 `CircuitBreaker` 过滤器，不需要在 `dev`、`prod` 的
每条路由上重复配置。网关中的这个过滤器由 `SentinelGatewayCircuitBreakerFilterFactory` 提供，内部会委托
Sentinel Reactive CircuitBreaker，并按 `routeId` 生成独立的熔断资源名和 fallback 地址。
`statusCodes` 会把下游 500、502、503、504 转成熔断统计的异常信号；触发阈值后，请求不再等待下游完整恢复，
而是进入对应的内部 Controller，并返回 HTTP 503：

```json
{
  "success": false,
  "code": 503,
  "message": "下游服务暂不可用，请稍后再试",
  "data": null,
  "timestamp": 1787191865837
}
```

熔断阈值说明：

- `min-request-amount`：进入熔断判断的最小请求数，避免少量请求误触发。
- `exception-ratio`：统计窗口内异常比例阈值。
- `slow-request-rt-threshold-ms`：慢请求 RT 阈值。
- `slow-request-ratio`：统计窗口内慢请求比例阈值。
- `stat-interval-ms`：熔断统计窗口。
- `recovery-seconds`：熔断后的半开放恢复等待时间。

`prod,remote` 模式下，Nacos 中的 `service-gateway` 分组、dataId 为 `prod` 配置只需要维护路由、CORS 等
环境差异；Sentinel 公共配置会随 `sentinel` profile 从应用包内加载，不需要在 Nacos 重复维护。注意不要在
Nacos 中重复定义 `spring.cloud.gateway.server.webflux.default-filters`，列表配置会被远端配置整体覆盖。
规则在应用启动时加载，调整阈值后需要重启网关。当前实现是应用内规则基线，尚未接入 Sentinel Dashboard
或 Nacos Sentinel datasource，规则调整后如需动态推送，可在此基础上继续扩展 datasource。

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
- spring-cloud-starter-alibaba-sentinel / spring-cloud-alibaba-sentinel-gateway
- spring-cloud-circuitbreaker-sentinel
- spring-cloud-starter-alibaba-nacos-discovery / config

## WebFlux 约束

Gateway 基于 WebFlux，不能与 `spring-boot-starter-web`（WebMVC）共存。引入 service-common 会带来 WebMVC 依赖，因此 Gateway
不依赖 common。
