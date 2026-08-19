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
└── GatewayApplication    启动类（@EnableDiscoveryClient）
```

## 路由配置

路由规则在 Nacos 上配置，dataId=`dev`，group=`service-gateway`。

## 配置说明

本地仅保留引导配置（端口、Nacos 地址），路由规则在 Nacos 中动态配置。Nacos 配置位置：dataId=`dev`，
group=`service-gateway`；当前引导配置地址为 `127.0.0.1:8848`。

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
