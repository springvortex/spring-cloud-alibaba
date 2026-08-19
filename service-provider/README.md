# Service Provider

服务提供者，项目的业务核心模块。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9001                                  |
| 服务名     | service-provider                      |
| Swagger UI | http://localhost:9001/swagger-ui.html |

## 接口

下表是 Controller 内编写的资源路径；服务启动时会根据 `spring.application.name=service-provider`
自动追加全局前缀，实际请求路径为 `/api/v1/provider + 资源路径`。

### 用户管理

| 方法   | 路径                           | 说明                 |
|--------|--------------------------------|----------------------|
| GET    | `/user/{id}`                   | 根据 ID 查询用户     |
| GET    | `/user/list`                   | 查询全部有效用户     |
| GET    | `/user/page?current=1&size=10` | 分页查询用户         |
| POST   | `/user`                        | 新增用户             |
| PUT    | `/user`                        | 修改用户             |
| DELETE | `/user/{id}`                   | 删除用户（逻辑删除） |

### 商品管理

| 方法   | 路径          | 说明                 |
|--------|---------------|----------------------|
| GET    | `/goods/{id}` | 根据 ID 查询商品     |
| GET    | `/goods/list` | 查询全部有效商品     |
| GET    | `/goods/page` | 分页查询商品         |
| POST   | `/goods`      | 新增商品             |
| PUT    | `/goods`      | 修改商品             |
| DELETE | `/goods/{id}` | 删除商品（逻辑删除） |

### 订单管理

| 方法   | 路径          | 说明                         |
|--------|---------------|------------------------------|
| GET    | `/order/{id}` | 查询单个订单（含明细聚合）   |
| GET    | `/order/list` | 查询全部有效订单（不含明细） |
| GET    | `/order/page` | 分页查询订单                 |
| POST   | `/order`      | 新增订单（仅主表）           |
| PUT    | `/order`      | 修改订单                     |
| DELETE | `/order/{id}` | 删除订单（逻辑删除）         |

### 连通性测试

| 方法 | 路径    | 说明                                              |
|------|---------|---------------------------------------------------|
| GET  | `/port` | 返回当前实例端口，供 consumer 通过 Feign 验证链路 |

## SpringDoc 分组

版本分组由 `service-common` 自动生成：

| 分组          | 路径匹配              |
|---------------|-----------------------|
| `v1-provider` | `/api/v1/provider/**` |

如需 v1/v2 共存，在 `zjc.api.versions` 中同时配置 `v1`、`v2`，并在 v2 Controller 上标注 `@ApiVersion("v2")`。

## 包结构

```
com.zjc.provider
├── ProviderApplication             启动类
├── config
│   ├── AuditMetaObjectHandler      自动填充 createTime / updateTime（时区 Asia/Shanghai）
│   ├── MybatisPlusConfig           分页插件
│   ├── NacosConfigListenerConfig   Nacos 配置变更监听
│   └── OpenApiConfig               SpringDoc 元信息配置
├── controller                      REST 接口（User/Goods/Order/Test）
├── converter                       MapStruct Entity/DTO 转换器
├── entity                          数据库实体（User/Goods/Order/OrderDetail）
├── mapper                          MyBatis-Plus Mapper
└── service / impl                  业务逻辑
```

## 自动继承的公共能力

引入 service-common 依赖后，本模块自动获得以下能力（无需配置）：

- **全局异常处理**：`GlobalExceptionHandler` 统一拦截异常并用 `ApiResponse` 包装返回
- **接口日志切面**：`WebLogAspect` 自动记录 Controller 入参、返回值与执行耗时

## 配置说明

配置来源由 `app.env` 与 `app.config.source` 组合控制，默认值为 `dev,local`。

- **local**：加载 `src/main/resources/config/application-dev.yaml` 或 `application-prod.yaml`，其中维护数据源、
  MyBatis-Plus 和 Swagger 开关。
- **remote**：通过 `src/main/resources/config/application-remote.yaml` 拉取 Nacos，dataId 为 `${zjc.config.env}`，
  group 为 `service-provider`，namespace 为 `public`。

Nacos 地址统一来自 `${zjc.infrastructure.host}:8848`，可通过 `INFRASTRUCTURE_HOST` 覆盖。local 模式只关闭 Nacos
Config，服务发现默认仍会注册到 Nacos。

## 日志与链路追踪

本模块通过 `service-config` 统一日志配置，日志输出到：

```text
logs/service-provider/
```

日志包含 `traceId` 和 `spanId`。模块同时引入 Actuator 与 Zipkin，通过 W3C `traceparent` 与 Gateway、Consumer 保持同一条链路：

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

排查请求时，先用日志中的 `traceId` 聚合本地日志，再到 Zipkin 查看完整调用树和耗时分布。

## 依赖

- service-common
- service-config
- spring-cloud-starter-alibaba-nacos-discovery / config
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-zipkin
- mybatis-plus-spring-boot4-starter
- mybatis-plus-jsqlparser
- mysql-connector-j
- mapstruct
- springdoc-openapi-starter-webmvc-ui
