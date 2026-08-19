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
- 通过 Nacos 进行动态路由配置
- 负载均衡（LoadBalancer + Caffeine 缓存）

## 包结构

```
com.zjc.gateway
├── GatewayApplication    启动类（@EnableDiscoveryClient）
└── filter
    └── ServiceGlobalFilter  全局请求日志过滤器
```

## 路由配置

路由规则在 Nacos 上配置，dataId=`dev`，group=`service-gateway`。

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

访问链路固定为：

```text
公网客户端 -> service-gateway -> lb://service-provider / service-consumer / service-mail
```

不要在安全组、防火墙或 Docker 端口映射中开放业务服务、Nacos 或 MySQL 端口。业务服务的独立地址和 Swagger UI
只用于本机或内网调试，生产环境统一通过 Gateway 访问 `/api/{版本}/{模块}/**`。

## 配置说明

本地仅保留引导配置（端口、Nacos 地址），路由规则在 Nacos 中动态配置。Nacos 配置位置：dataId=`dev`， group=`service-gateway`
；当前引导配置地址为 `127.0.0.1:8848`。

## 设计说明

网关作为纯路由转发层，保持轻量，不集成以下功能：

- 不依赖 service-common（避免 WebMVC 与 WebFlux 冲突）
- 不集成 SpringDoc / Swagger UI
- 不包含测试模块
- 不提供业务接口
- 不自动继承 GlobalExceptionHandler 和 WebLogAspect（WebFlux 不支持 `@RestControllerAdvice`）

> **注意**：如需在 Gateway 实现统一异常处理或请求日志，需编写 WebFlux 版本（`ErrorWebExceptionHandler` / `WebFilter`）。

## 依赖

- spring-cloud-starter-gateway-server-webflux
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- spring-cloud-starter-alibaba-nacos-discovery / config

## WebFlux 约束

Gateway 基于 WebFlux，不能与 `spring-boot-starter-web`（WebMVC）共存。引入 service-common 会带来 WebMVC 依赖，因此 Gateway
不依赖 common。
