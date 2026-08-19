# Service Config

统一配置模块，当前承载 Gateway、Provider、Consumer、Mail 共用的 Logback 配置，避免每个服务复制维护 `logback-spring.xml`。

## 模块定位

- 普通库模块，不独立启动，也不包含 Java 代码。
- 只维护 `src/main/resources/logback-spring.xml`。
- Spring Boot 会从依赖 JAR 的 classpath 根路径自动加载 `logback-spring.xml`，各服务仍使用自己的日志上下文和服务名。
- 不携带 WebMVC、WebFlux 或业务依赖，因此 Gateway 可以安全引入。
- `spring-boot.repackage.skip=true`，保持普通 JAR 并作为依赖打进各服务 Fat JAR。

## 接入方式

所有可运行服务都添加依赖：

```xml
<dependency>
    <groupId>com.zjc</groupId>
    <artifactId>service-config</artifactId>
    <version>1.0.0</version>
</dependency>
```

不要在业务模块的 `src/main/resources` 下再放同名 `logback-spring.xml`。服务模块如需临时覆盖，可以重新添加本地文件，但必须明确说明原因，避免公共配置失效。

## 日志格式

控制台与文件均输出：

```text
[时间] [线程] [traceId=...,spanId=...] [级别] [服务名:端口] [logger] [消息]
```

`traceId` 和 `spanId` 来自 Micrometer Tracing 写入的 MDC。没有追踪上下文时输出为空，例如应用启动早期或非 HTTP 后台任务。

## 日志目录

日志根目录由 `spring.application.name` 自动区分：

```text
logs/service-gateway/
logs/service-provider/
logs/service-consumer/
logs/service-mail/
```

每个服务内按级别归档：

```text
debug/   DEBUG 日志
info/    INFO 日志
warn/    WARN 日志
error/   ERROR 日志
```

滚动策略：

| 配置       | 值           |
|------------|--------------|
| 单文件大小 | 1MB          |
| 保留历史   | 180 天       |
| 总大小上限 | 每个级别 2GB |
| 文件编码   | UTF-8        |

## 异步策略

Logback 的 `AsyncAppender` 只允许挂载一个下游 Appender，因此每个级别都有独立的异步包装：

```xml

<appender name="ASYNC_INFO_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>8192</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <neverBlock>false</neverBlock>
    <includeCallerData>false</includeCallerData>
    <maxFlushTime>3000</maxFlushTime>
    <appender-ref ref="INFO_FILE"/>
</appender>
```

参数说明：

| 参数                      | 说明                                     |
|---------------------------|------------------------------------------|
| `queueSize`               | 每个级别的队列容量，当前均为 8192 条事件 |
| `discardingThreshold=0`   | 队列快满时不自动丢弃 INFO 及以下级别日志 |
| `neverBlock=false`        | 队列满时阻塞业务线程，优先保证日志完整   |
| `includeCallerData=false` | 不采集调用者类名和行号，降低异步开销     |
| `maxFlushTime=3000`       | 停机时最多等待 3 秒刷新队列              |

选择“不丢日志”的保守策略，适合问题排查和审计场景。如果某些高吞吐场景更看重可用性，可在评估后调整 `neverBlock`
，但要意识到队列满时可能丢日志。

## Profile 行为

| Profile        | 控制台   | 文件     | 说明                           |
|----------------|----------|----------|--------------------------------|
| `dev` / `test` | 同步输出 | 异步输出 | 控制台保持实时，便于 IDEA 调试 |
| `prod`         | 不输出   | 异步输出 | 减少容器标准输出和磁盘双写     |

开发/测试环境对 `com.zjc` 打开 DEBUG：

```xml

<logger name="com.zjc" level="DEBUG"/>
```

第三方依赖仍保持 INFO，避免 Nacos、MyBatis、Spring 等框架日志淹没业务日志。

## 维护约定

- 修改日志格式、目录、异步参数时，只修改本模块。
- 新增可运行服务时，添加 `service-config` 依赖即可，不需要复制日志文件。
- 保持文件名为 `logback-spring.xml`，因为配置使用了 `springProperty` 和 `springProfile`。
- 各服务必须设置有效 profile（`dev`、`test` 或 `prod`），否则当前配置不会激活任何 root appender。
