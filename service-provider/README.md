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

### 通用约定

- 分页参数 `current` 从 `1` 开始，`size` 取值范围为 `1-100`。
- 更新和删除接口以实际影响行数判断结果；记录不存在或未命中时返回业务失败 `code=102`、
  `message=资源不存在`，HTTP 状态仍保持统一响应封装的 `200`。

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
| POST   | `/goods/{id}/purchase` | 购买商品（分布式锁扣库存并创建订单） |
| PUT    | `/goods`      | 修改商品             |
| DELETE | `/goods/{id}` | 删除商品（逻辑删除） |

### 订单管理

| 方法   | 路径          | 说明                             |
|--------|---------------|----------------------------------|
| GET    | `/order/{id}` | 查询单个订单（含明细聚合）       |
| GET    | `/order/list` | 查询全部有效订单（不含明细）     |
| GET    | `/order/page` | 分页查询订单                     |
| POST   | `/order`      | 新增订单（主表与明细同事务）     |
| PUT    | `/order`      | 修改订单                         |
| DELETE | `/order/{id}` | 删除订单及明细（同事务逻辑删除） |

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
│   ├── OpenApiConfig               SpringDoc 元信息配置
│   └── RedissonConfiguration       Redisson 客户端配置
├── controller                      REST 接口（User/Goods/Order/Test）
├── converter                       MapStruct Entity/DTO 转换器
├── entity                          数据库实体（User/Goods/Order/OrderDetail）
├── mapper                          MyBatis-Plus Mapper
└── service / impl                  业务逻辑
```

## 详情缓存

provider 是用户、商品和订单数据的拥有方，因此第一层 Redis 缓存放在本模块的 Service 层，而不是 Controller 层， 也不通过
consumer 再包一层缓存，避免同一条数据出现双层缓存和失效不同步。

当前只缓存稳定读取的单个资源：

| 数据     | cacheName           | Key     | TTL     |
|----------|---------------------|---------|---------|
| 用户详情 | `provider:user:id`  | 用户 ID | 30 分钟 |
| 商品详情 | `provider:goods:id` | 商品 ID | 30 分钟 |

最终 Redis key 形如 `zjc:provider:user:id:1`。查询不存在时也会缓存空值，减少不存在的 ID 对数据库的穿透压力； 更新、删除成功后按
ID 驱逐对应详情缓存。用户/商品列表、分页和订单聚合查询暂不缓存。

Redis 读/写异常时业务请求会继续查数据库；缓存清理失败会输出 ERROR 日志，提示旧数据可能保留到 TTL 到期。 序列化、key 前缀、TTL
和降级策略由 `service-common` 的缓存自动装配统一提供。

### 购买接口

购买接口使用 `POST /api/v1/provider/goods/{id}/purchase`，请求体：

```json
{
  "userId": 1,
  "quantity": 1
}
```

购买流程按商品 ID 加 Redisson 可重入锁，key 为 `zjc:provider:goods:purchase:lock:{goodsId}`；锁等待 3 秒，超时返回业务码
`503` 和“当前购买人数过多，请稍后再试”。锁内使用数据库条件更新 `stock >= quantity` 原子扣库存，并创建订单主表和明细；库存不足返回
“库存不足，请稍后再试”。购买成功后会清理该商品详情缓存，响应包含订单号、金额和剩余库存。

## Redisson

provider 引入 Redisson Core，用于后续秒杀、延迟任务和跨实例协调场景，例如可重入锁、看门狗、延迟队列和分布式限流。客户端不使用
starter 自动装配，而是由 `RedissonConfiguration` 复用 `spring.data.redis` 的 host、port、database、认证和超时配置；dev/prod
不需要额外维护 Redisson 地址。

客户端启用懒初始化，只有第一次执行命令时才建立连接。看门狗基础值默认 30 秒：

```yaml
zjc:
  redisson:
    enabled: true
    lock-watchdog-timeout: 30s
```

使用 `lock()`、`tryLock(...)` 且不传 `leaseTime` 时，Redisson 会自动续期；一旦显式传入 `leaseTime`，锁到期后自动释放，不再由看门狗
续期。Redisson 对象名不经过 Spring Cache 的 key 前缀处理，业务代码需自行保持单冒号风格，例如：

```text
zjc:provider:seckill:lock:1001
zjc:provider:seckill:delay-queue
zjc:provider:ratelimit:user:10001
```

## 自动继承的公共能力

引入 service-common 依赖后，本模块自动获得以下能力（无需配置）：

- **全局异常处理**：`GlobalExceptionHandler` 统一拦截异常并用 `ApiResponse` 包装返回
- **接口日志切面**：`WebLogAspect` 自动记录 Controller 入参、返回值与执行耗时
- **Redis 缓存基础设施**：由 `RedisCacheAutoConfiguration` 提供统一 JSON 序列化、key 前缀、TTL 和故障降级

## 配置说明

配置由本地 Profile 控制，默认激活 `dev`。

- **dev**：加载 `src/main/resources/application-dev.yaml`，其中维护共享开发环境数据源、MyBatis-Plus 和 Swagger 开关。
- **prod**：加载 `src/main/resources/application-prod.yaml`，其中维护生产数据源与文档开关。

基础设施地址按环境固定：dev 使用 `129.204.226.206`，prod 使用 `127.0.0.1`；MySQL 均要求 SSL。Nacos 仅用于服务注册与发现，
`spring.cloud.nacos.config.enabled` 保持为 `false`。

公共缓存配置来自 `config/application-redis.yaml`：使用 Redis Cache，默认 TTL 30 分钟，用户/商品详情各自 30 分钟。 Redis
地址按环境维护：dev 为 `129.204.226.206:6379`，prod 为 `127.0.0.1:6379`； 两个环境的密码均使用 Jasypt 密文，启动时通过
`JASYPT_ENCRYPTOR_PASSWORD` 解密。Redisson 与 Spring Cache 共用这组连接配置。

## 日志与链路追踪

本模块使用 Spring Boot 默认日志配置，日志输出到服务进程标准输出。

日志包含 `traceId` 和 `spanId`。模块同时引入 Actuator 与 Zipkin，通过 W3C `traceparent` 与 Gateway、Consumer 保持同一条链路：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    export:
      zipkin:
        enabled: true
        endpoint: http://129.204.226.206:9411/api/v2/spans
      enabled: true
```

排查请求时，先用日志中的 `traceId` 聚合本地日志，再到 Zipkin 查看完整调用树和耗时分布。
`prod` Profile 会将采样率覆盖为 `0.1`，并关闭接口文档。

## 依赖

- service-common
- spring-cloud-starter-alibaba-nacos-discovery
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-data-redis
- spring-boot-starter-cache
- redisson
- spring-boot-starter-zipkin
- mybatis-plus-spring-boot4-starter
- mybatis-plus-jsqlparser
- mysql-connector-j
- mapstruct
- springdoc-openapi-starter-webmvc-ui
