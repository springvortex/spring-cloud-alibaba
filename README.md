# Spring Cloud Alibaba

基于 Spring Cloud Alibaba 的微服务学习与实践项目。

## 技术栈

| 分类           | 选型                                 | 版本       |
|----------------|--------------------------------------|------------|
| 语言           | Java                                 | 21         |
| 构建工具       | Maven                                | 3.9+       |
| 基础框架       | Spring Boot                          | 4.1.0      |
| 微服务框架     | Spring Cloud                         | 2025.1.2   |
| 注册中心       | Spring Cloud Alibaba Nacos           | 2025.1.0.0 |
| ORM            | MyBatis-Plus                         | 3.5.17     |
| 数据库         | MySQL                                | -          |
| 网关           | Spring Cloud Gateway                 | -          |
| 服务调用       | OpenFeign                            | -          |
| 客户端负载均衡 | Spring Cloud LoadBalancer + Caffeine | -          |
| 接口文档       | SpringDoc OpenAPI（Swagger UI）      | 3.1.0      |
| 配置加密       | Jasypt                               | 4.0.4      |
| 对象映射       | MapStruct                            | 1.6.3      |
| 服务容错       | Alibaba Sentinel                     | -          |
| 链路追踪       | Micrometer Tracing + Brave           | -          |
| 追踪后端       | Zipkin                               | -          |
| 日志框架       | SLF4J + Logback                      | -          |
| 工具库         | Hutool                               | 5.8.47     |
| 测试           | JUnit 5 + Mockito + AssertJ          | -          |
| 覆盖率         | JaCoCo                               | 0.8.15     |

## 模块说明

```
spring-cloud-alibaba
├── service-common      公共模块，存放 DTO、统一响应、常量、Feign 共享 API，业务服务依赖（Gateway 除外）
├── service-provider    服务提供者，端口 9001，用户/商品/订单业务
├── service-consumer    服务消费者，端口 9002，通过 Feign 调用 provider
├── service-gateway     API 网关，端口 80，统一入口与路由
├── service-mail        邮件服务，端口 9004，统一发送邮件，记录入库
└── MP-Generator        MyBatis-Plus 代码生成器，按数据库表生成 Entity/Mapper/Service
```

各模块根目录下均有独立的 README.md，记录该模块的职责、依赖、接口和配置说明。

父 POM 只聚合 `service-common` 和 4 个可运行服务；`MP-Generator` 是独立工具模块，
需要在 `MP-Generator` 目录单独执行 Maven 命令。

## 快速开始

### 组件清单

| 组件       | 必需                   | 默认端口               | 用途                                   | 安装建议                                                                        |
|------------|------------------------|------------------------|----------------------------------------|---------------------------------------------------------------------------------|
| JDK 21+    | 是                     | -                      | 编译与运行 Java 服务                   | Windows/macOS 使用 Temurin、Oracle JDK 等发行版；Linux 使用发行版包或解压发行版 |
| Maven 3.9+ | 构建必需               | -                      | 编译、测试、打包                       | `mvn -version` 确认可用；IDEA 可使用 Bundled Maven                              |
| MySQL 8+   | 是                     | 3306                   | 业务数据、邮件记录                     | 官方安装包或 Docker；生产环境仅内网访问                                         |
| Nacos 3.x  | 是                     | 8848、9848、9849、7848 | 服务注册与发现                         | 官方发行包或 Docker；开发可用 standalone + Derby，生产建议外置 MySQL            |
| Zipkin     | 可选，链路追踪建议安装 | 9411                   | 展示 trace/span 调用链                 | 官方发行包或 Docker；不安装时可将导出开关设为 false                             |
| SMTP 服务  | service-mail 必需      | 视服务商而定           | 发送邮件                               | 使用已有邮箱服务商 SMTP，凭据放本地 Profile 并用 Jasypt 加密                    |

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
2. Zipkin 打开 `http://127.0.0.1:9411` 确认可访问。
3. 需要访问公网组件时，只放行必要入口；Nacos、MySQL、Zipkin 不建议直接暴露公网。

当前仓库没有维护数据库初始化 SQL 文件，业务表结构需要从现有环境导出，或自行按下方表清单创建后再导入数据。

### 配置来源

各服务使用本地配置文件，`application.yaml` 中默认激活 `dev` Profile：

```bash
# dev：本地开发配置
java -jar service-provider-1.0.0.jar

# prod：生产配置
java -jar service-provider-1.0.0.jar --spring.profiles.active=prod
```

当前支持 `dev/prod` 两个环境。服务包内的 `application.yaml` 提供服务名、端口、默认环境和公共 Profile；
Nacos 公共认证来自 `config/application-nacos.yaml`，`application-{env}.yaml` 提供各环境差异配置。

项目当前主要使用两组环境 Profile：

| Profile | 说明 |
|---------|------|
| `dev` | 本地开发配置；开启 SpringDoc 与网关聚合 Swagger UI |
| `prod` | 生产配置；默认关闭 `/v3/api-docs` 与 Swagger UI，网关不注册 OpenAPI 转发路由，也不开放通配 CORS |

Nacos、MySQL、Zipkin 的主机地址按环境直接写入各服务的 Profile：`dev` 使用 `129.204.226.206`，
`prod` 使用 `127.0.0.1`。

Nacos 认证只作用于服务发现与注册。`dev` 与 `prod` 均使用 `config/application-nacos.yaml`
中的同一组账号密码；`application-{env}.yaml` 只维护各环境的服务器地址差异。

各服务已移除 Nacos Config 依赖，并显式设置 `spring.cloud.nacos.config.enabled=false`。Nacos 仅提供服务发现与注册，
连接地址按环境分别为 `129.204.226.206:8848` 与 `127.0.0.1:8848`；如需完全脱离 Nacos 运行单个服务，可显式设置
`spring.cloud.nacos.discovery.enabled=false`。

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

各服务使用 Spring Boot 默认日志配置，日志输出到服务进程标准输出，并包含 Micrometer Tracing 注入的
`traceId` 与 `spanId`。生产环境建议由容器运行时或进程管理器统一收集与轮转。

### 链路追踪

四个可运行服务均引入：

```text
spring-boot-starter-actuator
spring-boot-starter-zipkin
```

基于 Micrometer Tracing + Brave 生成 trace/span，并把数据导出到 Zipkin。Gateway 和业务服务都会传播 W3C `traceparent`
请求头，因此一次请求在网关、Provider、Consumer、Mail 中会保持同一个 `traceId`。

基础配置：

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

上述配置固定开启链路导出；`dev` Profile 使用全采样，`prod` Profile 使用 `0.1` 采样率，
并将 Zipkin 地址覆盖为 `http://127.0.0.1:9411/api/v2/spans`。

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
# 在项目根目录执行，构建父 POM 聚合的全部服务模块
mvn clean package -DskipTests

# 需要同时执行单元测试时
mvn clean package

# 构建独立代码生成器模块
cd MP-Generator && mvn package
```

### 打包后的输出结构

```
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
- **依赖隔离**：每个业务服务的依赖都打在各自 Fat JAR 内，gateway 的 WebFlux 栈与其他服务的 MVC 栈天然隔离

> **注意**：部署时不需要拷贝 `service-common` JAR，它已经作为依赖打进每个业务服务的 Fat JAR。

### 配置位置

所有打包配置都在父 `pom.xml` 的 `<plugins>` 中统一管理（非 pluginManagement），子模块自动继承，新建业务模块无需在 pom
中添加任何打包配置。

---

## 部署

### 前提条件

- JDK 21+（服务器上只需 JRE/JDK，不需要 Maven）
- 服务器能访问 Nacos（服务注册与发现）和 MySQL
- 开启链路追踪时，服务器能访问 Zipkin；Zipkin 本身也应部署在内网
- 应用配置（`application.yaml` 与环境 Profile）已打在 JAR 内，默认激活 `dev`，生产部署时切换到 `prod`

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

Linux 上非 root 用户不能绑定 1024 以下端口。Gateway 默认端口 80，需要改为高位端口（如 9000），可通过
`SERVER_PORT` 环境变量或启动参数覆盖 `server.port`。

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

项目集成 Jasypt 对敏感配置（数据库密码、SMTP 密码等）进行加密，密文以 `ENC(xxx)` 格式存储在本地环境 Profile 中。
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
  算法: PBEWithHMACSHA512AndAES_256
======================================
请输入要加密的明文: my-db-password

明文: my-db-password
密文: ENC(g48ZFqzM2yvuAMjOMw7z77DB7jTw9JjTkcJcuvo+Zkc=)

验证解密: my-db-password
匹配: true

将上面的 ENC(xxx) 复制到对应服务的环境 Profile 中即可。
```

### 在本地 Profile 中使用加密

将密文粘贴到对应服务的 `application-dev.yaml` 或 `application-prod.yaml` 中替换明文，例如：

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

Jasypt 加密参数（算法、迭代次数、salt 生成器等）统一来自各业务服务的 `config/application-jasypt.yaml`。

```yaml
jasypt:
  encryptor:
    algorithm: PBEWithHMACSHA512AndAES_256
    key-obtention-iterations: 100000
    pool-size: 1
    provider-name: SunJCE
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
    string-output-type: base64
```

> **注意**：jasypt-spring-boot-starter 4.0.4 已适配 Spring Boot 4.x。项目仍显式声明这些参数，避免依赖隐式默认值。

> **安全跟踪**：`CVE-2026-9370 / GHSA-jgj7-c8vj-w563` 标记 Jasypt 4.0.4 的 GCM 密钥派生存在可预测 salt 风险，
> 上游暂未发布 fixed 版本。当前项目显式配置 `RandomSaltGenerator` 与 `RandomIvGenerator`，不依赖默认派生参数；生产环境仍应确保
> `JASYPT_ENCRYPTOR_PASSWORD` 只通过环境变量或启动参数注入，并关注上游新版本，发布后立即升级验证。

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

通过 MyBatis-Plus 的 `logic-delete-field: isDeleted` 配置实现。该值对应实体属性名，实体属性再映射到数据库列
`is_deleted`；删除操作将 `is_deleted` 置为 1，查询时自动过滤已删除记录。

### 代码生成

`MP-Generator` 模块连接数据库读取表结构，生成 Entity/Mapper/Service/ServiceImpl/XML。

配置模板：`MP-Generator/src/main/resources/generator.properties.template`。首次使用时先复制为本地
`generator.properties`，后者被 Git 忽略，不会提交真实数据库凭据。

```properties
# 要生成的表
generator.tables=t_user,t_order,t_order_detail,t_goods
```

直接运行 `CodeGenerator#main` 即可生成，tinyint 字段统一生成 Integer 类型。生成结果输出到
`MP-Generator/src/main/java` 与 `MP-Generator/src/main/resources/mapper`，需要再迁移到实际业务模块；当前配置的父包是
`com.zjc.provider`，对应迁移目标为 `service-provider`。

## 配置管理

所有业务配置都随服务 JAR 打包。各服务 `application.yaml` 保留服务名、端口、默认环境和稳定公共配置，并通过
`spring.profiles.include` 引入 `nacos`、`api`、`jasypt`、`zipkin` 等公共 profile；根目录 `application-dev.yaml`
与 `application-prod.yaml` 维护环境差异。Gateway 不使用 API 前缀和 Jasypt，只 include `nacos`、`zipkin` 和 `sentinel`。

Nacos 不保存业务配置，也不参与配置导入；服务启动时只通过 Nacos Discovery 注册实例并发现下游服务。修改环境配置后，
需要重新打包并重启对应服务。

## 接口文档

本地开发使用 `dev` profile 时，各服务的开发配置会开启 SpringDoc，前端只需要访问网关聚合页：

```text
http://localhost/swagger-ui.html
```

页面中可在 `Provider v1`、`Consumer v1`、`Mail v1` 之间切换。聚合地址由网关的 `application-dev.yaml`
维护，例如 Provider 的 OpenAPI JSON 为：

```text
/api/v1/provider/v3/api-docs/v1-provider
```

所有业务服务统一使用 `/api/{版本}/{模块}` 前缀，模块名由 `service-{module}` 自动解析。 Controller 只编写资源路径；SpringDoc
按版本自动生成分组，例如 `v1-provider`。

生产使用 `prod` profile 时，生产配置保持 `springdoc.api-docs.enabled=false` 和
`springdoc.swagger-ui.enabled=false`，网关也不会加载 OpenAPI 转发路由。因此生产环境
不能通过 `/swagger-ui.html`、`/v3/api-docs` 或网关聚合地址查看接口文档。

验证时不要只看业务服务的 HTTP 状态码：WebMVC 服务的全局异常处理会把不存在的路径包装成 HTTP 200，
响应体为 `code=102`、`message=资源不存在`。SpringDoc 关闭后业务接口文档地址返回该响应；Gateway 自身则返回 404。

> **注意**：生产 Profile 不应重新设置 `springdoc.*.enabled=true`，避免生产文档被重新打开。

## 单元测试

项目使用 JUnit 5 + Mockito + AssertJ 编写单元测试。绝大多数测试不启动完整应用、不依赖 Nacos/MySQL；
`service-common` 中与自动装配相关的测试会使用轻量级 `WebApplicationContextRunner`。JaCoCo 会在测试执行后生成覆盖率报告。

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

分页参数 `current >= 1`、`size` 为 `1-100`；更新/删除未命中记录时返回业务失败
`code=102`（资源不存在）。
