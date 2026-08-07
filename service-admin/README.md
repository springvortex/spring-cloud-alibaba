# Service Admin

后台管理模块，当前为骨架结构，后续扩展管理端业务接口。

## 基本信息

| 项 | 值 |
|----|-----|
| 端口 | 9003 |
| 服务名 | service-admin |
| Swagger UI | http://localhost:9003/swagger-ui.html |

## 包结构

```
com.zjc.admin
├── AdminApplication          启动类（@EnableDiscoveryClient）
└── config
    └── OpenApiConfig         SpringDoc 文档配置
```

## 配置说明

本地仅保留引导配置，业务配置在 Nacos。
Nacos 配置位置：dataId=`dev`，group=`service-admin`

## 依赖

- service-common
- spring-boot-starter-web
- springdoc-openapi-starter-webmvc-ui
