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

### Nacos 配置测试

| 方法 | 路径      | 说明                                             |
|------|-----------|--------------------------------------------------|
| GET  | `/config` | 返回 Nacos 动态配置 `demo.msg:pub.name` 拼接结果 |

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
├── controller                       REST 接口（UserConsumer/TestFeign/TestConfig）
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

Nacos 配置位置：dataId=`dev`，group=`service-consumer`；当前引导配置地址为 `127.0.0.1:8848`。

OpenFeign 超时配置（在 Nacos 中）：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 10000
```

## 依赖

- service-common
- spring-cloud-starter-alibaba-nacos-discovery / config
- spring-boot-starter-web
- spring-cloud-starter-openfeign
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- springdoc-openapi-starter-webmvc-ui
