# Spring Cloud Alibaba

基于 Spring Cloud Alibaba 的微服务学习与实践项目。

## 技术栈

| 分类 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 基础框架 | Spring Boot | 4.0.0 |
| 微服务框架 | Spring Cloud | 2025.1.0 |
| 注册/配置中心 | Spring Cloud Alibaba Nacos | 2025.1.0.0 |
| ORM | MyBatis-Plus | 3.5.17 |
| 数据库 | MySQL | - |
| 网关 | Spring Cloud Gateway | - |
| 接口文档 | SpringDoc OpenAPI（Swagger UI） | 3.1.0 |
| 工具库 | Hutool | 5.8.46 |
| 测试 | JUnit 5 + Mockito + AssertJ | - |
| 覆盖率 | JaCoCo | 0.8.12 |

## 模块说明

```
spring-cloud-alibaba
├── service-common      公共模块，存放 DTO、统一响应、常量、Feign 共享 API，所有微服务依赖
├── service-provider    服务提供者，端口 9001，用户/商品/订单业务
├── service-consumer    服务消费者，端口 9002，通过 Feign 调用 provider
├── service-gateway     API 网关，端口 80，统一入口与路由
├── service-admin       后台管理，端口 9003
├── service-mail        邮件服务，端口 9004，统一收发邮件，记录入库
└── MP-Generator        MyBatis-Plus 代码生成器，按数据库表生成 Entity/Mapper/Service
```

各模块根目录下均有独立的 README.md，记录该模块的职责、依赖、接口和配置说明。

## 快速开始

### 前置依赖

- JDK 21+
- Maven 3.9+
- MySQL 8+
- Nacos 2.x（默认地址 `127.0.0.1:8848`，账号密码 `nacos/nacos`）

### 数据库

项目数据库名 `spring_cloud_alibaba`，主要业务表：

- `t_user` 用户表
- `t_goods` 商品表
- `t_order` 订单主表
- `t_order_detail` 订单明细表
- `t_mail_log` 邮件发送记录表

字段中 `is_deleted` 为逻辑删除标记，状态类字段（status/gender）使用 tinyint，Java 实体统一映射为 Integer。

### 启动顺序

1. 启动 Nacos
2. `service-provider`（业务核心）
3. `service-gateway`（可选，按需）
4. `service-consumer` / `service-admin` / `service-mail`（按需）

### 构建

```bash
# 根目录构建全部模块
mvn clean install -DskipTests

# 单独构建某个模块（含依赖模块）
mvn clean install -pl service-provider -am -DskipTests
```

## 核心约定

### 分层职责

- **Controller**：接收 HTTP 请求，调用 Service，负责 Entity→DTO 转换，对外只暴露 DTO
- **Service**：继承 MyBatis-Plus 的 IService，承载业务逻辑，内部使用 Entity
- **Mapper**：继承 BaseMapper，提供单表 CRUD

### Entity 与 DTO 分离

Entity（如 `com.zjc.provider.entity`）映射数据库表，仅模块内部使用，不对外暴露。
对外传输统一使用 DTO（`com.zjc.common.dto`），放在 common 模块供所有服务依赖。

DTO 相比 Entity 过滤了 `isDeleted`、`updateTime` 等内部字段，避免数据库结构泄露到接口契约中。

### 逻辑删除

通过 MyBatis-Plus 的 `logic-delete-field: is_deleted` 配置实现，删除操作将 `is_deleted` 置为 1，查询时自动过滤已删除记录。

### 代码生成

`MP-Generator` 模块连接数据库读取表结构，生成 Entity/Mapper/Service/ServiceImpl/XML。

配置文件：`MP-Generator/src/main/resources/generator.properties`

```properties
# 要生成的表
generator.tables=t_user,t_order,t_order_detail,t_goods
# 输出目标模块（相对聚合工程根）
generator.outputModule=service-provider
```

直接运行 `CodeGenerator#main` 即可生成，tinyint 字段统一生成 Integer 类型。

## 配置中心

各服务通过 Nacos 管理配置，本地 `application.yaml` 只保留引导信息（端口、Nacos 地址），
业务配置（数据源、MyBatis-Plus、SMTP 等）存放在 Nacos。

配置规则：
- **dataId**：激活环境名（如 `dev`、`test`、`prod`）
- **group**：服务名（如 `service-provider`、`service-mail`）
- **namespace**：`public`
- **热更新**：`refreshEnabled=true`

Nacos 地址：`127.0.0.1:8848`

每个服务本地有 `application.yaml`（端口、profile）和 `config/application-nacos.yaml`（Nacos 地址、config.import 变量）两个引导文件，
运行时通过 `${spring.profiles.active}` 和 `${spring.application.name}` 动态拼接拉取 Nacos 上对应环境的配置。

## 接口文档

所有业务模块均集成了 SpringDoc OpenAPI，启动后访问各模块的 Swagger UI：

| 模块 | 地址 |
|------|------|
| service-provider | `http://localhost:9001/swagger-ui.html` |
| service-consumer | `http://localhost:9002/swagger-ui.html` |
| service-admin | `http://localhost:9003/swagger-ui.html` |
| service-mail | `http://localhost:9004/swagger-ui.html` |
| service-gateway | `http://localhost:80/swagger-ui.html` |

网关支持聚合下游各服务的 API 文档，在 Nacos 的 `service-gateway` 配置中添加 `springdoc.swagger-ui.urls` 即可在网关 Swagger UI 顶部下拉框切换查看。

## 单元测试

项目使用 JUnit 5 + Mockito + AssertJ 编写纯单元测试（不启动 Spring 上下文、不依赖 Nacos/MySQL），
通过 JaCoCo 自动生成覆盖率报告。

### 命名规范

- 测试类：`被测类名 + Test`，如 `UserControllerTest`
- 测试方法：`test + 描述`，小驼峰命名，如 `testGetUserReturnsDto`
- 每个测试方法均含 Javadoc 注释，说明验证目标

### 测试覆盖范围

| 模块 | 测试类 | 用例数 | 覆盖率 |
|------|--------|--------|--------|
| service-common | `ApiResponseTest` | 15 | 100% |
| service-provider | `UserControllerTest` `GoodsControllerTest` `OrderControllerTest` `TestControllerTest` `AuditMetaObjectHandlerTest` `MybatisPlusConfigTest` `OpenApiConfigTest` | 33 | 95% |
| service-consumer | `UserFeignFallbackFactoryTest` `FeignServiceImplTest` `TessFeignControllerTest` `TestConfigControllerTest` `UserConsumerControllerTest` | 8 | 89% |
| service-mail | `MailSendServiceImplTest` `MailControllerTest` `MybatisPlusConfigTest` `AuditMetaObjectHandlerTest` | 11 | - |
| service-gateway | `GatewayApplicationTest` | 1 | - |
| service-admin | `AdminApplicationTest` | 1 | - |

### 运行测试

```bash
# 运行全部测试并生成 JaCoCo 覆盖率报告
mvn test

# 单独运行某个模块的测试
mvn test -pl service-provider
```

覆盖率报告生成在各模块 `target/site/jacoco/index.html`，浏览器打开即可查看。

## 接口示例

以用户接口为例（service-provider，端口 9001）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/{id}` | 查询单个用户 |
| GET | `/user/list` | 查询全部用户 |
| GET | `/user/page?current=1&size=10` | 分页查询 |
| POST | `/user` | 新增用户 |
| PUT | `/user` | 修改用户 |
| DELETE | `/user/{id}` | 删除用户（逻辑删除） |

所有接口统一返回 `ApiResponse`，结构为 `success + code + message + data + timestamp`。
