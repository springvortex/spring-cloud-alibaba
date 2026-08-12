# Service Admin（Spring Boot Admin Server）

Spring Boot Admin 监控面板服务，通过 Nacos 服务发现自动监控所有注册的微服务实例，提供可视化的健康状态、JVM 指标、日志级别管理等监控能力。

## 基本信息

| 项         | 值                                    |
|------------|---------------------------------------|
| 端口       | 9003                                  |
| 服务名     | service-admin                         |
| SBA 面板   | http://localhost:9003（需登录）       |
| Swagger UI | http://localhost:9003/swagger-ui.html |

## 监控架构

本模块作为 SBA Server，通过 `@EnableAdminServer` + Nacos Discovery 自动发现并监控以下服务（含自身）：

| 服务             | 端口 | 说明                   |
|------------------|------|------------------------|
| service-admin    | 9003 | SBA Server（监控自身） |
| service-provider | 9001 | 业务提供者             |
| service-consumer | 9002 | 业务消费者             |
| service-gateway  | 80   | API 网关（WebFlux）    |
| service-mail     | 9004 | 邮件服务               |

被监控服务无需引入 SBA Client 依赖，只需暴露 Actuator 端点（各服务 `application.yaml` 已配置
`management.endpoints.web.exposure.include: '*'`）。

### SBA 面板功能

- **Wallboard**：所有服务健康状态一览
- **Details**：单个服务的完整信息（JVM 内存/线程、日志级别、环境变量、缓存、SBOM 依赖清单等）
- **Loggers**：在线调整日志级别（无需重启）
- **Health**：健康检查详情（数据库连接、磁盘空间、Nacos 连接等）
- **Metrics**：JVM 指标、HTTP 请求统计等
- **SBOM**：软件物料清单（CycloneDX 格式，展示组件依赖关系与版本）

## 安全认证（Spring Security）

SBA 面板已集成 Spring Security，访问需登录认证。

### 凭证配置

登录凭证通过环境变量注入，不写入配置文件：

| 配置项 | 环境变量       | 默认值 | 说明                             |
|--------|----------------|--------|----------------------------------|
| 用户名 | `SBA_USERNAME` | admin  | 登录用户名                       |
| 密码   | `SBA_PASSWORD` | 无     | 登录密码（必填，不设则启动报错） |

### 传入凭证

**IDEA 本地开发**：Run Configuration -> VM Options

```
-DSBA_PASSWORD=your-password
```

**环境变量**：

```bash
# macOS / Linux
export SBA_PASSWORD=your-password
./service.sh start service-admin

# Windows PowerShell
$env:SBA_PASSWORD = "your-password"
.\service.bat start service-admin
```

> **安全提醒**：密码禁止写死在配置文件中，仅通过环境变量或 VM 参数注入。

## SpringDoc 分组

| 分组      | 路径匹配 |
|-----------|----------|
| 01-管理端 | `/**`    |

## 包结构

```
com.zjc.admin
├── AdminApplication                启动类（@EnableDiscoveryClient + @EnableAdminServer）
├── config
│   ├── OpenApiConfig               SpringDoc 文档配置
│   └── SecurityConfig              Spring Security 安全配置（SBA 面板登录认证）
```

## 自动继承的公共能力

引入 service-common 依赖后，本模块自动获得以下能力（无需配置）：

- **全局异常处理**：`GlobalExceptionHandler` 统一拦截异常并用 `ApiResponse` 包装返回
- **接口日志切面**：`WebLogAspect` 自动记录 Controller 入参、返回值与执行耗时

## 配置说明

本地 `application.yaml` 保留引导配置（端口、profile）和 Actuator 端点暴露配置，业务配置在 Nacos。

Nacos 配置位置：dataId=`dev`，group=`service-admin`

## 依赖

- service-common
- spring-boot-admin-starter-server（自带 web + actuator）
- spring-boot-starter-security（SBA 面板登录认证）
- springdoc-openapi-starter-webmvc-ui

> SBA Server starter 自带 `spring-boot-starter-web` 和 `spring-boot-starter-actuator`，无需额外声明。SBOM 数据由父 pom 的
> `cyclonedx-maven-plugin` 在编译期自动生成。