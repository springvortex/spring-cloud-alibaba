# Spring Cloud Alibaba

基于 Spring Cloud Alibaba 的微服务学习与实践项目。

## 技术栈

| 分类          | 选型                            | 版本       |
|---------------|---------------------------------|------------|
| 语言          | Java                            | 21         |
| 基础框架      | Spring Boot                     | 4.1.0      |
| 微服务框架    | Spring Cloud                    | 2025.1.2   |
| 注册/配置中心 | Spring Cloud Alibaba Nacos      | 2025.1.0.0 |
| ORM           | MyBatis-Plus                    | 3.5.17     |
| 数据库        | MySQL                           | -          |
| 网关          | Spring Cloud Gateway            | -          |
| 接口文档      | SpringDoc OpenAPI（Swagger UI） | 3.1.0      |
| 配置加密      | Jasypt                          | 3.0.5      |
| 服务监控      | Spring Boot Admin               | 4.1.2      |
| 工具库        | Hutool                          | 5.8.47     |
| 测试          | JUnit 5 + Mockito + AssertJ     | -          |
| 覆盖率        | JaCoCo                          | 0.8.15     |

## 模块说明

```
spring-cloud-alibaba
├── service-common      公共模块，存放 DTO、统一响应、常量、Feign 共享 API，所有微服务依赖
├── service-provider    服务提供者，端口 9001，用户/商品/订单业务
├── service-consumer    服务消费者，端口 9002，通过 Feign 调用 provider
├── service-gateway     API 网关，端口 80，统一入口与路由
├── service-admin       服务监控面板（Spring Boot Admin），端口 9003
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

---

## 打包

项目采用 **Thin JAR + 外部 lib** 的打包方式：每个服务只打自身代码（约 260-300 KB），依赖 JAR 单独输出到 `lib/` 目录，实现 JAR
包与依赖分离。

### 打包命令

```bash
# 在项目根目录执行，构建全部模块
mvn clean package -DskipTests
```

打包后的输出结构：

```
build/
├── service-admin/
│   ├── service-admin-1.0.0.jar          # Thin JAR（自身代码 + Spring Boot Loader）
│   └── lib/                             # 该服务所有 runtime 依赖（约 131 个 JAR）
├── service-common/
│   ├── service-common-1.0.0.jar         # 普通 JAR（公共库，不启动）
│   └── lib/
├── service-consumer/
│   ├── service-consumer-1.0.0.jar
│   └── lib/                             # 约 142 个 JAR
├── service-gateway/
│   ├── service-gateway-1.0.0.jar
│   └── lib/                             # 约 112 个 JAR（WebFlux 栈，与其他服务隔离）
├── service-mail/
│   ├── service-mail-1.0.0.jar
│   └── lib/                             # 约 159 个 JAR
└── service-provider/
    ├── service-provider-1.0.0.jar
    └── lib/                             # 约 153 个 JAR
```

### 打包原理

- **spring-boot-maven-plugin**：`repackage` 目标，`layout=ZIP`（PropertiesLauncher），`includes` 只含模块自身代码。thin JAR 通过
  `-Dloader.path=lib` 从外部 lib 目录加载依赖
- **maven-dependency-plugin**：`copy-dependencies` 目标，将所有 runtime 依赖复制到各服务的 `lib/` 目录
- **每个服务独立 lib**：gateway 是 WebFlux 栈，其余服务是 Spring MVC 栈，依赖天然冲突，必须隔离

> **注意**：`service-common` 是公共库模块，不执行 repackage（通过 `spring-boot.repackage.skip=true` 跳过），保持标准 JAR
> 供其他模块 Maven 依赖。部署时不需要拷贝 `build/service-common/`，它已经作为依赖打包在各服务的 `lib/` 中。

### 配置位置

所有打包配置都在父 `pom.xml` 的 `<plugins>` 中统一管理（非 pluginManagement），子模块自动继承，新建业务模块无需在 pom
中添加任何打包配置。

---

## 部署

### 前提条件

- JDK 21+（服务器上只需 JRE/JDK，不需要 Maven）
- 服务器能访问 Nacos（注册中心 + 配置中心）和 MySQL
- 应用配置（`application.yaml`）已打在 JAR 内，Nacos 上的业务配置启动时自动拉取

### 需要拷贝的文件

| 文件/目录     | 说明                                         |
|---------------|----------------------------------------------|
| `service.bat` | Windows 启动脚本                             |
| `service.ps1` | Windows 核心脚本（被 service.bat 调用）      |
| `service.sh`  | macOS/Linux 启动脚本                         |
| `build/`      | 全部服务目录（排除 `build/service-common/`） |

部署后的目录结构：

```
deploy/
├── service.bat              # Windows 脚本
├── service.ps1
├── service.sh               # macOS/Linux 脚本
└── build/
    ├── service-gateway/
    │   ├── service-gateway-1.0.0.jar
    │   └── lib/
    ├── service-provider/
    │   ├── service-provider-1.0.0.jar
    │   └── lib/
    ├── service-consumer/
    │   ├── service-consumer-1.0.0.jar
    │   └── lib/
    ├── service-admin/
    │   ├── service-admin-1.0.0.jar
    │   └── lib/
    └── service-mail/
        ├── service-mail-1.0.0.jar
        └── lib/
```

> 脚本和 `build/` 目录必须放在同一层级。脚本通过相对路径 `./build` 或 `.\build` 查找服务。

### Windows 部署

#### 1. 拷贝文件

将 `service.bat`、`service.ps1` 和 `build/`（排除 `service-common`）拷贝到部署目录。

#### 2. 确保 Java 可用

```powershell
java -version
# 确认输出 Java 21
```

#### 3. 启动服务

```bat
rem 交互式菜单（双击 service.bat 也可以）
service.bat

rem 命令行直接操作
service.bat start all                      rem 一键启动全部
service.bat start service-provider          rem 启动单个服务
service.bat stop service-provider           rem 停止单个服务
service.bat restart service-gateway         rem 重启单个服务
service.bat stop all                        rem 一键停止全部
service.bat status                          rem 查看所有服务状态
```

> **注意**：PowerShell 中需要加 `.\` 前缀：`.\service.bat start all`。CMD 中直接 `service.bat start all` 即可。

### macOS / Linux 部署

#### 1. 拷贝文件

将 `service.sh` 和 `build/`（排除 `service-common`）拷贝到部署目录。

#### 2. 赋予脚本执行权限

```bash
chmod +x service.sh
```

#### 3. 确保 Java 可用

```bash
java -version
# 确认输出 Java 21
```

#### 4. 启动服务

```bash
# 交互式菜单
./service.sh

# 命令行直接操作
./service.sh start all                     # 一键启动全部
./service.sh start service-provider         # 启动单个服务
./service.sh stop service-provider          # 停止单个服务
./service.sh restart service-gateway        # 重启单个服务
./service.sh stop all                       # 一键停止全部
./service.sh status                         # 查看所有服务状态
```

### 交互式菜单

双击 `service.bat`（Windows）或执行 `./service.sh`（macOS/Linux）进入交互式菜单：

```
  ==================================================
    Spring Cloud Alibaba - Service Manager
  ==================================================

  [1] service-admin          STOPPED
  [2] service-consumer       STOPPED
  [3] service-gateway        RUNNING  (PID: 31332)
  [4] service-mail           STOPPED
  [5] service-provider       RUNNING  (PID: 17552)

  Jasypt: ENABLED

  [a] Start all    [s] Stop all    [x] Restart all
  [r] Refresh      [q] Quit
```

| 操作               | 输入                     |
|--------------------|--------------------------|
| 启动全部服务       | `a`                      |
| 停止全部服务       | `s`                      |
| 重启全部服务       | `x`                      |
| 启动/停止/重启单个 | 输入编号，进入子菜单操作 |
| 刷新状态           | `r`                      |
| 退出               | `q`                      |

菜单自动扫描 `build/` 下的服务目录（排除 `service-common`），无需手动配置服务列表。菜单还会显示 Jasypt
密钥是否已加载（ENABLED / DISABLED）。

### 进程与日志

- **PID 文件**：`build/<service>/<service>.pid`，记录进程号，用于精确启停
- **stdout 日志**：`build/<service>/<service>.out`
- **stderr 日志**：`build/<service>/<service>.err`

排查启动问题时，先看 `.err` 文件，再看 `.out` 文件末尾。

### 部分部署

生产环境如果只需要部分服务（如 gateway + provider），只需拷贝对应的 `build/<service>/` 目录，脚本自动扫描，菜单只显示已部署的服务。

### Linux 端口注意事项

Linux 上非 root 用户不能绑定 1024 以下端口。Gateway 默认端口 80，需要改为高位端口（如 9000），或在 Nacos 配置中心修改
`server.port`。

### JVM 参数调整

默认 JVM 参数 `-Xms256m -Xmx512m`，可在脚本顶部修改：

- `service.ps1`：`$JVM_OPTS = "-Xms256m -Xmx512m"`
- `service.sh`：`JVM_OPTS="-Xms256m -Xmx512m"`

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
# macOS / Linux
./service.sh start all jasypt.encryptor.password=your-secret-key

# Windows
.\service.bat start all jasypt.encryptor.password=your-secret-key
```

**方式二：环境变量**

```bash
# macOS / Linux
export JASYPT_ENCRYPTOR_PASSWORD=your-secret-key
./service.sh start all

# Windows PowerShell
$env:JASYPT_ENCRYPTOR_PASSWORD = "your-secret-key"
.\service.bat start all
```

菜单界面会显示 Jasypt 是否已启用（`ENABLED` / `DISABLED`），不显示密钥本身。

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

> **注意**：jasypt-spring-boot-starter 3.0.5 的默认值与上述参数完全一致，但在 Spring Boot 4.x
> 下，默认值解析存在兼容性问题，可能导致解密失败。因此显式声明这些参数并集中放在 Nacos 公共配置组中，所有服务共享同一份配置，便于维护和动态刷新。

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
# 输出目标模块（相对聚合工程根）
generator.outputModule=service-provider
```

直接运行 `CodeGenerator#main` 即可生成，tinyint 字段统一生成 Integer 类型。

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

Nacos 地址：`127.0.0.1:8848`

每个服务本地有 `application.yaml`（端口、profile）和 `config/application-nacos.yaml`（Nacos 地址、config.import
变量）两个引导文件，运行时通过 `${spring.profiles.active}` 和 `${spring.application.name}` 动态拼接拉取 Nacos 上对应环境的配置。

## 服务监控（SBA）

项目使用 [Spring Boot Admin](https://docs.spring-boot-admin.com/) (SBA) 4.1.2 作为服务监控面板。`service-admin` 模块作为
SBA Server，通过 Nacos 服务发现自动监控所有注册的微服务。

### 工作原理

- **SBA Server**（service-admin）：启用 `@EnableAdminServer`，通过 Nacos Discovery 自动发现所有注册的实例
- **被监控服务**：无需引入 SBA Client 依赖，只需暴露 Actuator 端点即可被 SBA 自动发现和采集

各服务本地 `application.yaml` 中已配置 Actuator 端点暴露：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```

> 此配置也可迁移到 Nacos 公共配置组（group: `spring-cloud-alibaba-public`）集中管理，与 Jasypt 配置同理。

### 访问面板

启动 `service-admin` 后，浏览器访问：

```
http://localhost:9003
```

SBA 面板提供以下功能：

- **Wallboard**：所有服务健康状态一览
- **Details**：单个服务的完整信息（JVM 内存/线程、日志级别、环境变量、缓存等）
- **Loggers**：在线调整日志级别（无需重启）
- **Health**：健康检查详情（数据库连接、磁盘空间、Nacos 连接等）
- **Metrics**：JVM 指标、HTTP 请求统计等

### 监控范围

SBA 通过 Nacos 发现以下服务（含 SBA Server 自身）：

| 服务             | 端口 | 说明                   |
|------------------|------|------------------------|
| service-admin    | 9003 | SBA Server（监控自身） |
| service-provider | 9001 | 业务提供者             |
| service-consumer | 9002 | 业务消费者             |
| service-gateway  | 80   | API 网关（WebFlux）    |
| service-mail     | 9004 | 邮件服务               |

> **安全提醒**：当前 SBA 面板未配置安全认证，生产环境请集成 Spring Security 添加登录保护。

## 接口文档

以下业务模块集成了 SpringDoc OpenAPI，启动后访问各模块的 Swagger UI：

| 模块             | 地址                                    |
|------------------|-----------------------------------------|
| service-provider | `http://localhost:9001/swagger-ui.html` |
| service-consumer | `http://localhost:9002/swagger-ui.html` |
| service-admin    | `http://localhost:9003/swagger-ui.html` |
| service-mail     | `http://localhost:9004/swagger-ui.html` |

各模块 SpringDoc 分组按业务划分，Swagger UI 顶部下拉框可切换。

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

| 方法   | 路径                           | 说明                 |
|--------|--------------------------------|----------------------|
| GET    | `/user/{id}`                   | 查询单个用户         |
| GET    | `/user/list`                   | 查询全部用户         |
| GET    | `/user/page?current=1&size=10` | 分页查询             |
| POST   | `/user`                        | 新增用户             |
| PUT    | `/user`                        | 修改用户             |
| DELETE | `/user/{id}`                   | 删除用户（逻辑删除） |

所有接口统一返回 `ApiResponse`，结构为 `success + code + message + data + timestamp`。