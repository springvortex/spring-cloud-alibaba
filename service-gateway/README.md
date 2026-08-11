# Service Gateway

API 网关，基于 Spring Cloud Gateway（WebFlux），统一入口与路由。

## 基本信息

| 项     | 值              |
|--------|-----------------|
| 端口   | 80              |
| 服务名 | service-gateway |

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

本地仅保留引导配置（端口、Nacos 地址），路由规则在 Nacos 中动态配置。 Nacos 配置位置：dataId=`dev`，group=`service-gateway`

## 设计说明

网关作为纯路由转发层，保持轻量，不集成以下功能：

- 不依赖 service-common（避免 WebMVC 与 WebFlux 冲突）
- 不集成 SpringDoc / Swagger UI
- 不包含测试模块
- 不提供业务接口

## 依赖

- spring-cloud-starter-gateway-server-webflux
- spring-cloud-starter-loadbalancer
- caffeine（LoadBalancer 缓存）
- spring-cloud-starter-alibaba-nacos-discovery / config

## 注意

Gateway 基于 WebFlux，不能与 `spring-boot-starter-web`（WebMVC）共存。
