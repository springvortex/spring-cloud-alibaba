# Service Consumer

服务消费者，通过 Feign 远程调用 provider，展示微服务间通信。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9002                                  |
| 服务名     | service-consumer                      |
| Swagger UI | http://localhost:9002/swagger-ui.html |

## 接口

下表是 Controller 资源路径，实际请求路径自动追加 `/api/v1/consumer` 前缀。

### 用户消费

通过 Feign 代理调用 provider 的用户接口，provider 不可用时自动降级。

| 方法 | 路径         | 说明                             |
|------|--------------|----------------------------------|
| GET  | `/user/{id}` | 远程查询用户（Feign + 降级演示） |
| GET  | `/user/list` | 远程查询用户列表                 |

### Feign 测试

| 方法 | 路径          | 说明                              |
|------|---------------|-----------------------------------|
| GET  | `/feign/port` | 通过 Feign 远程获取 provider 端口 |

## SpringDoc 分组

版本分组由 `service-common` 自动生成：

| 分组          | 路径匹配              |
|---------------|-----------------------|
| `v1-consumer` | `/api/v1/consumer/**` |

## 包结构

```
com.zjc.consumer
├── ConsumerApplication              启动类（扫描 com.zjc.common.api 中的 Feign 契约）
├── config
│   └── OpenApiConfig                SpringDoc 元信息配置
├── controller                       REST 接口（UserConsumer/TestFeign）
└── service / impl                   FeignService 封装
```

## Feign 降级机制

用户调用复用 `service-common` 中的 `UserFeignApi`，其 `UserFeignFallbackFactory` 也由 common 自动注册。provider 不可用或调用超时
时，单个用户查询返回空 `data`，用户列表返回空列表，上层 Controller 无需 try-catch。

`/feign/port` 使用 common 中的 `TestApi`。consumer 通过
`@EnableFeignClients(basePackages = {"com.zjc.common.api"})` 扫描共享契约，本地没有重复定义 Feign 客户端。

## 自动继承的公共能力

引入 service-common 依赖后，本模块自动获得以下能力（无需配置）：

- **全局异常处理**：`GlobalExceptionHandler` 统一拦截异常并用 `ApiResponse` 包装返回
- **接口日志切面**：`WebLogAspect` 自动记录 Controller 入参、返回值与执行耗时

## 配置说明

配置由本地 Profile 控制，默认激活 `dev`。`src/main/resources/application-dev.yaml` 维护开发环境
OpenFeign 与 Swagger 配置，`src/main/resources/application-prod.yaml` 维护生产环境 OpenFeign 超时配置。

Nacos 与 Zipkin 地址按环境固定：dev 使用 `129.204.226.206`，prod 使用 `127.0.0.1`。Nacos 仅用于服务注册与发现，
`spring.cloud.nacos.config.enabled` 保持为 `false`。

生产环境 OpenFeign 超时配置结构如下，开发环境基线为 1000/2000 ms：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 3000
            read-timeout: 5000
```

## 日志与链路追踪

本模块使用 Spring Boot 默认日志配置，日志输出到服务进程标准输出。

日志包含 `traceId` 和 `spanId`。模块同时引入 Actuator 与 Zipkin；通过 Feign 调用 Provider 时，会继续传播 W3C `traceparent`
，Gateway、Consumer、Provider 可以在 Zipkin 中组成同一条调用链：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    export:
      zipkin:
        enabled: true
        endpoint: http://129.204.226.206:9411/api/v2/spans
      enabled: true
```

排查远程调用时，重点查看同一个 `traceId` 下 Consumer 发起请求和 Provider 处理请求的 span 耗时。

## 依赖

- service-common
- spring-cloud-starter-alibaba-nacos-discovery
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-zipkin
- spring-cloud-starter-openfeign
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- springdoc-openapi-starter-webmvc-ui
