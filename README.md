﻿# Spring Cloud Alibaba

基于 Spring Cloud Alibaba 的微服务学习与实践项目。

## 技术栈

| 分类          | 选型                            | 版本       |
|---------------|---------------------------------|------------|
| 语言          | Java                            | 21         |
| 构建工具      | Maven                           | 3.9+       |
| 基础框架      | Spring Boot                     | 4.1.0      |
| 微服务框架    | Spring Cloud                    | 2025.1.2   |
| 注册/配置中心 | Spring Cloud Alibaba Nacos      | 2025.1.0.0 |
| ORM           | MyBatis-Plus                    | 3.5.17     |
| 数据库        | MySQL                           | -          |
| 网关          | Spring Cloud Gateway            | -          |
| 服务调用      | OpenFeign                       | -          |
| 客户端负载均衡 | Spring Cloud LoadBalancer + Caffeine | - |
| 接口文档      | SpringDoc OpenAPI（Swagger UI） | 3.1.0      |
| 配置加密      | Jasypt                          | 4.0.4      |
| 对象映射      | MapStruct                       | 1.6.3      |
| 服务容错      | Alibaba Sentinel                | -          |
| 链路追踪      | Micrometer Tracing + Brave      | -          |
| 追踪后端      | Zipkin                          | -          |
| 日志框架      | SLF4J + Logback                 | -          |
| 工具库        | Hutool                          | 5.8.47     |
| 测试          | JUnit 5 + Mockito + AssertJ     | -          |
| 覆盖率        | JaCoCo                          | 0.8.15     |

## 模块说明

```
spring-cloud-alibaba
├── service-common      公共模块，存放 DTO、统一响应、常量、Feign 共享 API，业务服务依赖（Gateway 除外）
├── service-config      统一配置模块，当前维护所有服务共用的 logback-spring.xml
├── service-provider    服务提供者，端口 9001，用户/商品/订单业务
├── service-consumer    服务消费者，端口 9002，通过 Feign 调用 provider
├── service-gateway     API 网关，端口 80，统一入口与路由
├── service-mail        邮件服务，端口 9004，统一发送邮件，记录入库
└── MP-Generator        MyBatis-Plus 代码生成器，按数据库表生成 Entity/Mapper/Service
```

各模块根目录下均有独立的 README.md，记录该模块的职责、依赖、接口和配置说明。

## 快速开始

### 组件清单

| 组件 | 必需 | 默认端口 | 用途 | 安装建议 |
|------|------|----------|------|----------|
| JDK 21+ | 是 | - | 编译与运行 Java 服务 | Windows/macOS 使用 Temurin、Oracle JDK 等发行版；Linux 使用发行版包或解压发行版 |
| Maven 3.9+ | 构建必需 | - | 编译、测试、打包 | `mvn -version` 确认可用；IDEA 可使用 Bundled Maven |
| MySQL 8+ | 是 | 3306 | 业务数据、Nacos 生产配置存储、邮件记录 | 官方安装包或 Docker；生产环境仅内网访问 |
| Nacos 3.x | 是 | 8848、9848、9849、7848 | 注册中心、配置中心 | 官方发行包或 Docker；开发可用 standalone + Derby，生产建议外置 MySQL |
| Zipkin | 可选，链路追踪建议安装 | 9411 | 展示 trace/span 调用链 | 官方发行包或 Docker；不安装时可将导出开关设为 false |
| SMTP 服务 | service-mail 必需 | 视服务商而定 | 发送邮件 | 使用已有邮箱服务商 SMTP，凭据放 Nacos 并用 Jasypt 加密 |

### 组件安装

基础组件只需安装在开发机或内网服务器，不需要部署到每个应用目录。以下命令适合本地开发验证：

```bash
# MySQL 8
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=your-root-password \
  -e MYSQL_DATABASE=spring_cloud_alibaba \
  -p 3306:3306 \
  mysql:8

# Nacos standalone（开发环境；生产建议配置外置 MySQL 并开启鉴权）
docker run -d --name nacos \
  -e MODE=standalone \
  -p 8848:8848 -p 9848:9848 \
  nacos/nacos-server:v3.0.0

# Zipkin
docker run -d --name zipkin \
  -p 9411:9411 \
  openzipkin/zipkin:3
```

Windows 不使用 Docker 时，下载对应组件的压缩包或安装包，解压后按官方脚本启动；启动前确认 `java -version` 为 Java 21+。

上面的 Zipkin 容器默认将数据保存在内存中，重启后调用链会丢失；生产环境应配置 Elasticsearch、MySQL 等存储后端，并与应用放在同一内网。

组件启动后：

1. MySQL 创建数据库 `spring_cloud_alibaba` 并导入业务表。
2. Nacos 创建 `public` 命名空间下的服务配置和公共配置组。
3. Zipkin 打开 `http://127.0.0.1:9411` 确认可访问。
4. 需要访问公网组件时，只放行必要入口；Nacos、MySQL、Zipkin 不建议直接暴露公网。

### 本地配置

各服务的 Nacos 引导配置位于：

```text
service-{module}/src/main/resources/config/application-nacos.yaml
```

该文件包含 Nacos 地址、认证信息和 `spring.config.import`。本地开发时按实际环境修改；不建议把生产账号密码提交到仓库。

### 数据库

项目数据库名 `spring_cloud_alibaba`，主要业务表：

- `t_user` 用户表
- `t_goods` 商品表
- `t_order` 订单主表
- `t_order_detail` 订单明细表
- `t_mail_log` 邮件发送记录表

字段中 `is_deleted` 为逻辑删除标记，状态类字段（status/gender）使用 tinyint，Java 实体统一映射为 Integer。

### 启动顺序

1. 启动 MySQL、Nacos；如需查看调用链，同时启动 Zipkin
2. `service-provider`（业务核心）
3. `service-gateway`（统一入口，建议启动）
4. `service-consumer` / `service-mail`（按需）

---

## 日志与链路追踪

### 统一日志

所有可运行服务都依赖 `service-config`，共享一份：

```text
service-config/src/main/resources/logback-spring.xml
```

不要再在各业务模块复制 `logback-spring.xml`。日志目录由 `spring.application.name` 自动区分：

```text
logs/
├── service-gateway/
├── service-provider/
├── service-consumer/
└── service-mail/
```

每个服务目录下按级别拆分：

```text
debug/ info/ warn/ error/
```

日志格式包含：

```text
traceId、spanId、服务名、端口、线程、级别、logger、业务消息
```

开发/测试环境：控制台同步输出，方便 IDEA 实时查看；四个级别的文件输出分别异步。生产环境：不输出控制台，只保留异步文件输出。每个异步 Appender 均使用 `queueSize=8192`、`discardingThreshold=0`、`neverBlock=false`，队列满时优先保证日志不丢失。

更多说明见 [service-config/README.md](service-config/README.md)。

### 链路追踪

四个可运行服务均引入：

```text
spring-boot-starter-actuator
spring-boot-starter-zipkin
```

基于 Micrometer Tracing + Brave 生成 trace/span，并把数据导出到 Zipkin。Gateway 和业务服务都会传播 W3C `traceparent` 请求头，因此一次请求在网关、Provider、Consumer、Mail 中会保持同一个 `traceId`。

基础配置：

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
    export:
      zipkin:
        enabled: ${ZIPKIN_EXPORT_ENABLED:true}
        endpoint: "${ZIPKIN_ENDPOINT:http://127.0.0.1:9411/api/v2/spans}"
      enabled: ${TRACING_ENABLED:true}
```

环境变量说明：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | 采样比例，`1.0` 表示全采样，`0.1` 表示约 10% 请求生成可导出的追踪数据 |
| `ZIPKIN_EXPORT_ENABLED` | `true` | 是否导出 Zipkin |
| `ZIPKIN_ENDPOINT` | `http://127.0.0.1:9411/api/v2/spans` | Zipkin 上报地址 |
| `TRACING_ENABLED` | `true` | 当前配置在 `management.tracing.export.enabled` 下，只控制导出；若要整体关闭 tracing，应额外配置 `management.tracing.enabled` |

排查方式：

1. 通过 Gateway 发起请求，例如 `GET /api/v1/provider/user/1`。
2. 从 Gateway 开始/结束日志中复制 `traceId`。
3. 在 Zipkin 查询该 `traceId`，查看网关与下游服务的调用树、耗时和异常。
4. 也可用同一个 `traceId` 聚合各服务本地日志。

## 打包

项目采用 Spring Boot 标准 **Fat JAR** 打包方式：每个可启动服务的业务代码和全部运行依赖都打包在同一个 JAR 中，可直接通过
`java -jar` 启动，不再需要外部 `lib/` 目录和 `loader.path` 参数。

### 打包命令

```bash
# 在项目根目录执行，构建全部模块
mvn clean package -DskipTests

# 需要同时执行单元测试时
mvn clean package
```

打包后的输出结构：

```
service-config/target/
└── service-config-1.0.0.jar            # 普通 JAR，统一配置资源，不独立启动
service-common/target/
└── service-common-1.0.0.jar             # 普通 JAR，公共库，不独立启动
service-consumer/target/
└── service-consumer-1.0.0.jar           # 可执行 Fat JAR
service-gateway/target/
└── service-gateway-1.0.0.jar            # 可执行 Fat JAR
service-mail/target/
└── service-mail-1.0.0.jar               # 可执行 Fat JAR
service-provider/target/
└── service-provider-1.0.0.jar           # 可执行 Fat JAR
```

### 打包原理

- **业务服务模块**：继承父 POM 中的 `spring-boot-maven-plugin`，执行 `repackage` 后生成可执行 Fat JAR
- **service-common**：通过 `spring-boot.repackage.skip=true` 保持普通库 JAR，供其他模块 Maven 依赖，不独立部署
- **service-config**：同样保持普通库 JAR，只作为依赖打进各服务 Fat JAR，不独立部署
- **依赖隔离**：每个业务服务的依赖都打在各自 Fat JAR 内，gateway 的 WebFlux 栈与其他服务的 MVC 栈天然隔离

> **注意**：部署时不需要拷贝 `service-common` 和 `service-config` JAR，它们已经作为依赖打进每个业务服务的 Fat JAR。

### 配置位置

所有打包配置都在父 `pom.xml` 的 `<plugins>` 中统一管理（非 pluginManagement），子模块自动继承，新建业务模块无需在 pom
中添加任何打包配置。

---

## 部署

### 前提条件

- JDK 21+（服务器上只需 JRE/JDK，不需要 Maven）
- 服务器能访问 Nacos（注册中心 + 配置中心）和 MySQL
- 开启链路追踪时，服务器能访问 Zipkin；Zipkin 本身也应部署在内网
- 应用配置（`application.yaml`）已打在 JAR 内，Nacos 上的业务配置启动时自动拉取

### 需要拷贝的文件

| 文件                                  | 说明                    |
|---------------------------------------|-------------------------|
| `service-consumer/target/*-1.0.0.jar` | Consumer 可执行 Fat JAR |
| `service-gateway/target/*-1.0.0.jar`  | Gateway 可执行 Fat JAR  |
| `service-mail/target/*-1.0.0.jar`     | Mail 可执行 Fat JAR     |
| `service-provider/target/*-1.0.0.jar` | Provider 可执行 Fat JAR |

部署后的目录结构：

```
deploy/
├── service-gateway-1.0.0.jar
├── service-provider-1.0.0.jar
├── service-consumer-1.0.0.jar
└── service-mail-1.0.0.jar
```

> 只部署需要的服务即可；每个 Fat JAR 都是独立制品，不要求放在同一个目录。

### Windows 部署

#### 1. 拷贝文件

将需要部署的 Fat JAR 拷贝到部署目录。

#### 2. 确保 Java 可用

```powershell
java -version
# 确认输出 Java 21+
```

#### 3. 启动服务

```powershell
java -jar service-provider-1.0.0.jar
```

生产环境可后台启动，例如：

```powershell
$env:JASYPT_ENCRYPTOR_PASSWORD = "your-secret-key"
Start-Process -FilePath "java" `
  -ArgumentList "-Xms256m", "-Xmx512m", "-jar", "service-provider-1.0.0.jar" `
  -RedirectStandardOutput "provider.out" `
  -RedirectStandardError "provider.err"
```

### macOS / Linux 部署

#### 1. 拷贝文件

将需要部署的 Fat JAR 拷贝到部署目录。

#### 2. 确保 Java 可用

```bash
java -version
# 确认输出 Java 21+
```

#### 3. 启动服务

```bash
# 前台启动
java -jar service-provider-1.0.0.jar

# 后台启动，并记录 PID
export JASYPT_ENCRYPTOR_PASSWORD="your-secret-key"
nohup java -Xms256m -Xmx512m -jar service-provider-1.0.0.jar \
  > provider.out 2> provider.err &
echo $! > provider.pid
```

#### 4. 停止服务

```bash
kill "$(cat provider.pid)"
```

### 部分部署

生产环境如果只需要部分服务（如 gateway + provider），只拷贝对应的 Fat JAR 即可。

### Linux 端口注意事项

Linux 上非 root 用户不能绑定 1024 以下端口。Gateway 默认端口 80，需要改为高位端口（如 9000），或在 Nacos 配置中心修改
`server.port`。

### 网络边界与端口暴露策略

生产环境的访问入口应收敛到 Gateway：外部客户端只访问 Gateway，Gateway 再通过 Nacos 服务发现调用业务服务。 业务服务虽然需要监听
HTTP 端口供 Gateway 和其他内部服务调用，但这些端口必须留在内网，不能暴露到公网。

| 服务 / 组件      | 默认端口                      | 是否允许公网暴露 | 说明                                        |
|------------------|-------------------------------|------------------|---------------------------------------------|
| service-gateway  | 80（生产可用 80/443 或 9000） | 允许             | 系统唯一 HTTP 入口                          |
| service-provider | 9001                          | 禁止             | 仅 Gateway / 内部服务访问                   |
| service-consumer | 9002                          | 禁止             | 仅 Gateway / 内部服务访问                   |
| service-mail     | 9004                          | 禁止             | 仅 Gateway / 内部服务访问                   |
| Nacos            | 8848、9848、9849、7848        | 禁止             | 控制台、客户端通信、Raft 端口都不能公网开放 |
| MySQL            | 3306                          | 禁止             | 仅业务服务和运维链路访问                    |
| Zipkin           | 9411                          | 禁止             | 仅应用和运维人员内网访问                    |

推荐的网络拓扑：

```text
公网客户端
    |
    v
service-gateway（唯一暴露入口）
    |
    v
内网：service-provider / service-consumer / service-mail
    |
    v
内网：Nacos / MySQL / Zipkin
```

部署时的具体要求：

- 云服务器安全组、防火墙或负载均衡只放行 Gateway 的 `80/443`（若 Gateway 改为 9000，则只放行 9000）。
- `9001`、`9002`、`9004` 只允许内网访问；Docker 部署时不要把这些端口映射到宿主机公网地址。
- Nacos 的 `8848` 控制台以及 `9848`、`9849`、`7848` 通信端口都不得暴露公网。
- MySQL `3306` 不得暴露公网，应用通过内网地址连接。
- Zipkin `9411` 不得暴露公网；确需远程查看时通过 VPN、堡垒机或内网反向代理接入。
- 直接访问 `http://host:9001/swagger-ui.html`、`http://host:9002/swagger-ui.html`、
  `http://host:9004/swagger-ui.html` 只适用于本机或内网调试，生产环境不允许绕过 Gateway。
- Kubernetes 部署时业务服务使用 `ClusterIP` Service，只把 Gateway 暴露为 Ingress 或 LoadBalancer。

网络边界是“只能通过网关访问”的主要保障。应用层可再增加内部请求头、签名或 mTLS 校验，作为业务端口被误暴露时的兜底，
但不要用应用层校验替代防火墙和安全组隔离。

### JVM 参数调整

JVM 参数直接放在 `java` 命令和 `-jar` 之间：

```bash
java -Xms512m -Xmx1024m -jar service-provider-1.0.0.jar
```

---

## 配置加密（Jasypt）

项目集成 Jasypt 对敏感配置（数据库密码、SMTP 密码等）进行加密，密文以 `ENC(xxx)` 格式存储在 Nacos 配置中。
**密钥不写入任何配置文件**，通过命令行参数或环境变量注入。

### 加密明文

在 IDEA 中运行 `service-common` 模块的 `com.zjc.common.JasyptTest`，在 Run Configuration -> VM Options 中填入密钥：

```
-Djasypt.encryptor.password=your-secret-key
```

运行后输入明文，得到加密结果：

```
======================================
  Jasypt 加密工具
  算法: PBEWithMD5AndDES
======================================
请输入要加密的明文: my-db-password

明文: my-db-password
密文: ENC(g48ZFqzM2yvuAMjOMw7z77DB7jTw9JjTkcJcuvo+Zkc=)

验证解密: my-db-password
匹配: true

将上面的 ENC(xxx) 复制到 Nacos 配置文件中即可。
```

### 在 Nacos 中使用加密

将密文粘贴到 Nacos 配置中替换明文，例如：

```yaml
spring:
  datasource:
    password: ENC(g48ZFqzM2yvuAMjOMw7z77DB7jTw9JjTkcJcuvo+Zkc=)
```

Jasypt 会在应用启动时自动检测 `ENC()` 包裹的值并解密。

### 启动时传入密钥

密钥不写入任何配置文件，通过以下方式注入（优先级从高到低）：

**方式一：命令行参数（推荐）**

```bash
# macOS / Linux / Windows
java -Djasypt.encryptor.password=your-secret-key -jar service-provider-1.0.0.jar

# 也可使用 Spring Boot 命令行参数
java -jar service-provider-1.0.0.jar --jasypt.encryptor.password=your-secret-key
```

**方式二：环境变量**

```bash
# macOS / Linux
export JASYPT_ENCRYPTOR_PASSWORD=your-secret-key
java -jar service-provider-1.0.0.jar

# Windows PowerShell
$env:JASYPT_ENCRYPTOR_PASSWORD = "your-secret-key"
java -jar service-provider-1.0.0.jar
```

上述示例以 provider 为例，其他服务替换对应 JAR 文件名即可。

### 加密算法配置

Jasypt 加密参数（算法、迭代次数、salt 生成器等）统一存放在 Nacos 配置中心的公共配置组中，各服务通过 `config.import` 引入：

```yaml
# 各服务的 config/application-nacos.yaml
spring:
  config:
    import:
      - nacos:jasypt?group=spring-cloud-alibaba-public&namespace=public&refreshEnabled=true
```

Nacos 中 `jasypt` 配置内容（group: `spring-cloud-alibaba-public`）：

```yaml
jasypt:
  encryptor:
    algorithm: PBEWithMD5AndDES
    key-obtention-iterations: 1000
    pool-size: 1
    provider-name: SunJCE
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
    iv-generator-classname: org.jasypt.iv.NoIvGenerator
    string-output-type: base64
```

> **注意**：jasypt-spring-boot-starter 4.0.4 已适配 Spring Boot 4.x。项目仍显式声明这些参数，并集中放在
> Nacos 公共配置组中，避免依赖隐式默认值；所有服务共享同一份配置，也便于维护和动态刷新。

### 本地开发（IDEA）

在 IDEA 中运行时，需在 **Run Configuration -> VM Options** 中填入：

```
-Djasypt.encryptor.password=your-secret-key
```

> **常见问题**：如果通过 Windows 高级系统设置添加了 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量，必须 **彻底退出 IDEA
再重新打开**，否则 IDEA 不会继承新增的环境变量，导致解密失败。直接在 VM Options 中传 `-D` 参数是最可靠的方式。

---

## 核心约定

### 分层职责

- **Controller**：接收 HTTP 请求，调用 Service，负责 Entity->DTO 转换，对外只暴露 DTO
- **Service**：继承 MyBatis-Plus 的 IService，承载业务逻辑，内部使用 Entity
- **Mapper**：继承 BaseMapper，提供单表 CRUD

### Entity 与 DTO 分离

Entity（如 `com.zjc.provider.entity`）映射数据库表，仅模块内部使用，不对外暴露。对外传输统一使用 DTO（`com.zjc.common.dto`），放在
common 模块供所有服务依赖。

DTO 相比 Entity 过滤了 `isDeleted`、`updateTime` 等内部字段，避免数据库结构泄露到接口契约中。

### 逻辑删除

通过 MyBatis-Plus 的 `logic-delete-field: is_deleted` 配置实现，删除操作将 `is_deleted` 置为 1，查询时自动过滤已删除记录。

### 代码生成

`MP-Generator` 模块连接数据库读取表结构，生成 Entity/Mapper/Service/ServiceImpl/XML。

配置文件：`MP-Generator/src/main/resources/generator.properties`

```properties
# 要生成的表
generator.tables=t_user,t_order,t_order_detail,t_goods
```

直接运行 `CodeGenerator#main` 即可生成，tinyint 字段统一生成 Integer 类型。生成结果输出到
`MP-Generator/src/main/java` 与 `MP-Generator/src/main/resources/mapper`，需要再迁移到实际业务模块；当前配置的父包是
`com.zjc.provider`，对应迁移目标为 `service-provider`。

## 配置中心

各服务通过 Nacos 管理配置，本地 `application.yaml` 只保留引导信息（端口、Nacos 地址），业务配置（数据源、MyBatis-Plus、SMTP
等）存放在 Nacos。

配置规则：

- **dataId**：激活环境名（如 `dev`、`test`、`prod`）
- **group**：服务名（如 `service-provider`、`service-mail`）
- **namespace**：`public`
- **热更新**：`refreshEnabled=true`

公共配置组（group: `spring-cloud-alibaba-public`）存放所有服务共享的配置，如 Jasypt 加密算法参数（dataId: `jasypt`）。各服务通过
`config.import` 引入。

Nacos 地址：当前引导配置为 `127.0.0.1:8848`

每个服务本地有 `application.yaml`（端口、profile）和 `config/application-nacos.yaml`（Nacos 地址、config.import
变量）两个引导文件，运行时通过 `${spring.profiles.active}` 和 `${spring.application.name}` 动态拼接拉取 Nacos 上对应环境的配置。

## 接口文档

以下业务模块集成了 SpringDoc OpenAPI，启动后访问各模块的 Swagger UI：

| 模块             | 地址                                    |
|------------------|-----------------------------------------|
| service-provider | `http://localhost:9001/swagger-ui.html` |
| service-consumer | `http://localhost:9002/swagger-ui.html` |
| service-mail     | `http://localhost:9004/swagger-ui.html` |

所有业务服务统一使用 `/api/{版本}/{模块}` 前缀，模块名由 `service-{module}` 自动解析。 Controller 只编写资源路径；SpringDoc
按版本自动生成分组，例如 `v1-provider`。

> **注意**：Gateway 作为纯路由网关，不集成接口文档，保持轻量。

## 单元测试

项目使用 JUnit 5 + Mockito + AssertJ 编写纯单元测试（不启动 Spring 上下文、不依赖 Nacos/MySQL），通过 JaCoCo 自动生成覆盖率报告。

### 命名规范

- 测试类：`被测类名 + Test`，如 `UserControllerTest`
- 测试方法：`test + 描述`，小驼峰命名，如 `testGetUserReturnsDto`
- 每个测试方法均含 Javadoc 注释，说明验证目标

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

| 方法   | 实际请求路径                                   | 说明                 |
|--------|------------------------------------------------|----------------------|
| GET    | `/api/v1/provider/user/{id}`                   | 查询单个用户         |
| GET    | `/api/v1/provider/user/list`                   | 查询全部用户         |
| GET    | `/api/v1/provider/user/page?current=1&size=10` | 分页查询             |
| POST   | `/api/v1/provider/user`                        | 新增用户             |
| PUT    | `/api/v1/provider/user`                        | 修改用户             |
| DELETE | `/api/v1/provider/user/{id}`                   | 删除用户（逻辑删除） |

所有接口统一返回 `ApiResponse`，结构为 `success + code + message + data + timestamp`。
