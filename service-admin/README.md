# Service Admin

后台管理模块，当前为骨架结构，后续扩展管理端业务接口。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9003                                  |
| 服务名     | service-admin                         |
| Swagger UI | http://localhost:9003/swagger-ui.html |

## 接口

### 系统信息

| 方法 | 路径           | 说明                         |
|------|----------------|------------------------------|
| GET  | `/system/info` | 查询项目构建元数据与运行环境 |

## SpringDoc 分组

| 分组           | 路径匹配        |
|----------------|-----------------|
| 01-管理端      | `/**`           |
| 02-系统信息    | `/system/**`    |

## 包结构

```
com.zjc.admin
├── AdminApplication                启动类（@EnableDiscoveryClient）
├── config
│   └── OpenApiConfig               SpringDoc 文档配置
└── controller
    └── SystemInfoController        系统信息接口
```

## 配置说明

本地仅保留引导配置，业务配置在 Nacos。
Nacos 配置位置：dataId=`dev`，group=`service-admin`

## 构建信息

pom.xml 配置了 `spring-boot-maven-plugin` 的 `build-info` 目标，编译期生成 `META-INF/build-info.properties`，
供 `/system/info` 接口读取项目名称、版本、构建时间等元数据。

## 依赖

- service-common
- spring-boot-starter-web / actuator
- springdoc-openapi-starter-webmvc-ui
