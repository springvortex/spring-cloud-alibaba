# Service Gateway

API 网关，基于 Spring Cloud Gateway（WebFlux），统一入口与路由。

## 基本信息

| 项 | 值 |
|----|-----|
| 端口 | 80 |
| 服务名 | service-gateway |
| Swagger UI | http://localhost:80/swagger-ui.html |

## 职责

- 统一路由入口，将请求分发到下游各服务
- 聚合下游服务的 Swagger API 文档
- 通过 Nacos 进行动态路由配置

## 包结构

```
com.zjc.gateway
├── GatewayApplication        启动类
└── config
    └── OpenApiConfig         SpringDoc 文档元信息
```

## 路由配置

路由规则在 Nacos 上配置，dataId=`dev`，group=`service-gateway`。

## 聚合 API 文档

如需在网关 Swagger UI 中聚合各下游服务的接口文档，在 Nacos 的 `service-gateway` 配置中添加：

```yaml
springdoc:
  swagger-ui:
    urls:
      - name: 服务提供者
        url: /service-provider/v3/api-docs
      - name: 服务消费者
        url: /service-consumer/v3/api-docs
      - name: 邮件服务
        url: /service-mail/v3/api-docs
```

## 依赖

- spring-cloud-starter-gateway-server-webflux
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- spring-cloud-starter-alibaba-nacos-discovery / config
- springdoc-openapi-starter-webflux-ui

## 注意

Gateway 基于 WebFlux，不能与 `spring-boot-starter-web`（WebMVC）共存。接口文档使用 WebFlux 版本的 SpringDoc。
