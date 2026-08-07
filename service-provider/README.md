# Service Provider

服务提供者，项目的业务核心模块。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9001                                  |
| 服务名     | service-provider                      |
| Swagger UI | http://localhost:9001/swagger-ui.html |

## 业务模块

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

## 包结构

```
com.zjc.provider
├── ProviderApplication        启动类
├── config
│   ├── AuditMetaObjectHandler 自动填充 createTime / updateTime
│   ├── MybatisPlusConfig      分页插件
│   ├── NacosConfigListenerConfig  Nacos 配置变更监听
│   └── OpenApiConfig          SpringDoc 分组配置
├── controller                 REST 接口
├── entity                     数据库实体（User/Goods/Order/OrderDetail）
├── mapper                     MyBatis-Plus Mapper
└── service / impl             业务逻辑
```

## 配置说明

本地仅保留引导配置（端口、profile、Nacos 地址），数据源、MyBatis-Plus 等业务配置在 Nacos。

Nacos 配置位置：dataId=`dev`，group=`service-provider`

## 依赖

- service-common
- spring-boot-starter-web
- mybatis-plus-spring-boot4-starter
- mysql-connector-j
- springdoc-openapi-starter-webmvc-ui
