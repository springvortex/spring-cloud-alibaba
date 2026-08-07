# Service Consumer

服务消费者，通过 Feign 远程调用 provider，展示微服务间通信。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9002                                  |
| 服务名     | service-consumer                      |
| Swagger UI | http://localhost:9002/swagger-ui.html |

## 接口

### 用户消费

通过 Feign 代理调用 provider 的用户接口，provider 不可用时自动降级。

| 方法 | 路径                  | 说明                             |
|------|-----------------------|----------------------------------|
| GET  | `/consumer/user/{id}` | 远程查询用户（Feign + 降级演示） |
| GET  | `/consumer/user/list` | 远程查询用户列表                 |

### Feign 测试

| 方法 | 路径          | 说明                              |
|------|---------------|-----------------------------------|
| GET  | `/feign/port` | 通过 Feign 远程获取 provider 端口 |

### Nacos 配置测试

| 方法 | 路径      | 说明                                             |
|------|-----------|--------------------------------------------------|
| GET  | `/config` | 返回 Nacos 动态配置 `demo.msg:pub.name` 拼接结果 |

## 包结构

```
com.zjc.consumer
├── ConsumerApplication        启动类（开启 @EnableFeignClients）
├── config
│   └── OpenApiConfig          SpringDoc 分组配置
├── controller                 REST 接口
├── feign
│   ├── UserFeignClient        用户 Feign 客户端
│   └── factory
│       └── UserFeignFallbackFactory  降级工厂
└── service / impl             FeignService 封装
```

## Feign 降级机制

`UserFeignClient` 通过 `UserFeignFallbackFactory` 实现 fallback： 当 provider 不可用时，自动返回兜底数据，上层 Controller
无需 try-catch。

## 配置说明

Nacos 配置位置：dataId=`dev`，group=`service-consumer`

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
- spring-cloud-starter-openfeign
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- springdoc-openapi-starter-webmvc-ui
