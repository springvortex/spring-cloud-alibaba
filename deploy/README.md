# Docker 镜像打包与 Ubuntu 部署手册

这份手册描述当前项目从 Windows 构建镜像、传输到 VMware Ubuntu、加载镜像、启动服务、修改配置、扩容和新增模块的完整流程。

当前方案使用 Jib `buildTar` + `scp` + `docker load`，不需要注册 Docker Hub，也不需要私有镜像仓库。

## 1. 部署形态

当前部署形态如下：

```text
Windows 开发机
  ├── Maven + Jib buildTar：生成每个服务的 OCI 镜像 tar
  └── ssh/scp：传输镜像 tar 和部署文件

VMware Ubuntu 192.168.100.128
  ├── Docker Engine + Docker Compose
  ├── /home/zjc/zjc-app：Compose、外置配置、镜像 tar、业务日志
  ├── MySQL / Redis / Nacos / Zipkin / Mailhog：运行在宿主机网络
  └── 业务容器：provider、consumer、mail、gateway
```

网络规则：

| 服务 | 容器端口 | 是否发布到宿主机 | 说明 |
| --- | ---: | --- | --- |
| service-provider | 9001 | 否 | 只在 Docker 内部网络访问 |
| service-consumer | 9002 | 否 | 只在 Docker 内部网络访问 |
| service-mail | 9004 | 否 | 只在 Docker 内部网络访问 |
| service-gateway | 80 | 是，映射宿主机 80 | 外部流量统一从网关进入 |

`expose` 只是镜像/Compose 的端口声明，不会占用宿主机端口；`ports` 才会把容器端口发布到宿主机。当前只有 gateway 使用 `ports`。

## 2. 文件说明

```text
deploy/
  docker-compose.yml                         # Ubuntu 上运行的 Compose 文件
  .env.example                               # 环境变量模板，不含真实密钥
  .gitignore                                 # 排除真实 .env 和日志
  config/provider/application-vm.yaml
  config/consumer/application-vm.yaml
  config/gateway/application-vm.yaml
  config/mail/application-vm.yaml

scripts/
  deploy.ps1                                 # Windows 侧构建、传输、加载、启动脚本

service-provider/target/jib-image.tar        # provider 镜像 tar
service-consumer/target/jib-image.tar        # consumer 镜像 tar
service-gateway/target/jib-image.tar         # gateway 镜像 tar
service-mail/target/jib-image.tar            # mail 镜像 tar
```

镜像 tar 是构建产物，不应该提交 Git。真实 `.env` 和日志也只保存在部署机，不提交 Git。

## 3. 在 Windows 上打包镜像

### 3.1 前置条件

Windows 开发机需要：

```text
JDK 21
Maven
ssh / scp
```

执行 `buildTar` 不需要安装 Docker，也不需要启动 Docker Desktop。Jib 会直接生成 OCI 格式的 tar 文件。

在仓库根目录执行：

```powershell
mvn -Pdocker-tar "-Ddocker.tag=1.0.0" -DskipTests clean package
```

构建完成后会生成：

```text
service-provider\target\jib-image.tar
service-consumer\target\jib-image.tar
service-gateway\target\jib-image.tar
service-mail\target\jib-image.tar
```

只构建 provider 及其依赖模块：

```powershell
mvn -pl service-provider -am -Pdocker-tar "-Ddocker.tag=1.0.0" -DskipTests clean package
```

### 3.2 构建机制

公共 Jib 配置在根 `pom.xml`：

- 使用 `docker-tar` Profile 触发镜像构建。
- 父 POM 和 `service-common` 默认 `jib.skip=true`，不生成镜像。
- 每个可运行服务只在自己的 POM 里声明：
  - `docker.main-class`：启动类
  - `docker.container-port`：容器内端口
  - `jib.skip=false`：开启 buildTar
- 镜像名格式是 `zjc/<artifactId>:<tag>`。
- 平台固定为 `linux/amd64`，适配 VMware Ubuntu 的常见 x86_64 环境。
- 镜像内置 JVM 参数：
  - `Asia/Shanghai`
  - `UTF-8`
  - `optional:file:/app/config/` 外置配置目录
  - `MaxRAMPercentage=75`
  - OOM 后退出并保留 heap dump

默认基础镜像：

```text
dockerproxy.net/library/eclipse-temurin:21-jre
```

如果以后网络环境变化，可以在构建时覆盖：

```powershell
mvn -Pdocker-tar "-Ddocker.base-image=eclipse-temurin:21-jre" "-Ddocker.tag=1.0.0" -DskipTests clean package
```

### 3.3 镜像里有什么

镜像包含：

- 应用 class 文件
- 依赖 jar
- `src/main/resources` 下的内置配置
- 启动类和 JVM 参数

镜像不包含：

- `deploy/config` 下的外置配置
- 真实 `.env`
- 部署机上的日志

所以业务代码或内置配置变化时，需要重新构建镜像并重新加载；基础设施地址变化时，优先修改外置配置并重启容器。

## 4. 在 Ubuntu 上安装 Docker

以下命令在 Ubuntu 上执行。

### 4.1 安装 Docker Engine 和 Compose 插件

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

如果官方 Docker 仓库访问很慢，可以改用可用的镜像源或 Ubuntu 自带包，但必须保证最终这两个命令可用：

```bash
docker version
docker compose version
```

### 4.2 让当前用户可以直接使用 docker

```bash
sudo usermod -aG docker "$USER"
```

执行后需要退出 SSH 重新登录，或者重启 Ubuntu，让用户组生效。验证：

```bash
docker ps
```

如果不想把用户加入 docker 组，也可以所有命令都加 `sudo`，但后续脚本中的远程 Docker 命令也需要相应调整。

### 4.3 确认 CPU 架构

```bash
uname -m
```

预期是：

```text
x86_64
```

如果是 `aarch64`，当前 Jib 配置生成的 `linux/amd64` 镜像不能直接运行，需要修改根 POM 中的 platform 配置。

## 5. 传输镜像和部署文件

### 5.1 推荐方式：使用部署脚本

在 Windows 仓库根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -Build
```

默认目标是：

```text
zjc@192.168.100.128
/home/zjc/zjc-app
```

常用参数：

```powershell
# 只传输已经存在的镜像 tar，不重新构建
.\scripts\deploy.ps1

# 构建、传输，然后远程 docker load
.\scripts\deploy.ps1 -Build -Load

# 构建、传输、远程 docker load，并启动 docker compose
.\scripts\deploy.ps1 -Build -Start

# 自定义远程地址、目录和镜像 tag
.\scripts\deploy.ps1 -Build -Start -Remote "zjc@192.168.100.128" -AppDir "/home/zjc/zjc-app" -Tag "1.0.1"
```

脚本会自动从 `deploy/docker-compose.yml` 解析 `zjc/service-*` 镜像定义。以后新增模块后，只要 Compose 里出现了新的 `zjc/service-ai:${APP_TAG}`，脚本就会自动检查并传输 `service-ai\target\jib-image.tar`，不需要修改脚本。

注意：脚本不会创建或覆盖远程 `.env`，避免把真实密钥弄丢。`.env` 需要第一次手动创建。

### 5.2 手动传输

如果不使用脚本，可以在 Windows 上执行：

```powershell
$Remote = "zjc@192.168.100.128"
$AppDir = "/home/zjc/zjc-app"

ssh $Remote "mkdir -p $AppDir/images"

scp deploy\docker-compose.yml "$($Remote):$AppDir/docker-compose.yml"
scp deploy\.env.example "$($Remote):$AppDir/.env.example"
scp -r deploy\config "$($Remote):$AppDir/"

foreach ($module in @("provider", "consumer", "gateway", "mail")) {
    scp "service-$module\target\jib-image.tar" "$($Remote):$AppDir/images/service-$module.tar"
}
```

以后手动传输新模块时，把模块名加入数组即可。

## 6. 首次启动

### 6.1 创建 `.env`

在 Ubuntu 上执行：

```bash
cd /home/zjc/zjc-app
cp .env.example .env
chmod 600 .env
vi .env
```

需要确认三个值：

```text
APP_TAG=1.0.0
SPRING_PROFILE=dev,vm
JASYPT_ENCRYPTOR_PASSWORD=真实主密钥
```

`APP_TAG` 必须和镜像构建时的 `-Ddocker.tag` 一致。

`SPRING_PROFILE=dev,vm` 表示：

- `dev` 读取镜像内置的开发环境基线配置。
- `vm` 读取 `/app/config/application-vm.yaml`，覆盖虚拟机环境中的基础设施地址。

如果以后要以生产基线运行，可以改成：

```text
SPRING_PROFILE=prod,vm
```

此时仍然可以通过 `vm` 外置配置覆盖生产环境里的具体地址。

`JASYPT_ENCRYPTOR_PASSWORD` 只写在部署机的 `.env` 中，不要提交 Git，也不要输出到日志、终端截图或聊天记录里。

### 6.2 加载镜像

```bash
cd /home/zjc/zjc-app

for module in provider consumer gateway mail; do
  docker load -i "images/service-$module.tar"
done
```

检查镜像：

```bash
docker images "zjc/service-*"
```

### 6.3 启动服务

```bash
docker compose up -d
docker compose ps
```

查看日志：

```bash
# 跟踪某个服务
docker compose logs -f service-provider

# 查看最近日志
docker compose logs --tail=200 service-gateway

# 查看全部服务
docker compose logs -f
```

业务文件日志在宿主机：

```text
/home/zjc/zjc-app/logs/provider
/home/zjc/zjc-app/logs/consumer
/home/zjc/zjc-app/logs/mail
/home/zjc/zjc-app/logs/gateway
```

`SPRING_PROFILE=dev,vm` 时，日志会同时输出到控制台和文件，所以 `docker compose logs` 方便排查。`SPRING_PROFILE=prod,vm` 时，Logback 只保留异步文件日志，业务日志要以宿主机挂载目录为准。

验证网关：

```bash
curl http://192.168.100.128/api/v1/provider/user/1
curl http://192.168.100.128/api/v1/consumer/user/1
```

如果 Ubuntu 开启了 UFW，需要放行宿主机 80 端口：

```bash
sudo ufw allow 80/tcp
```

### 6.4 停止和重启

```bash
docker compose start
docker compose stop
docker compose restart service-provider
docker compose restart
docker compose down
```

`stop` 只停止容器，容器定义还保留，后续用 `start` 拉起。`down` 会停止并删除容器、删除 Compose 创建的网络，但不会删除镜像、`.env`、外置配置和日志。`up -d` 会按当前 Compose 文件和 `.env` 创建或重建容器。

`stop_grace_period` 已比应用优雅停机等待时间多留了 10 秒。停止或删除容器时，Docker 会先发 `SIGTERM`，等待应用处理完请求和注册信息，再强制停止。

更完整的启停、更新、配置变更、扩缩容和清理流程见第 12 章《日常维护手册》。

## 7. 外置配置

### 7.1 生效机制

每个镜像的 JVM 参数里都有：

```text
-Dspring.config.additional-location=optional:file:/app/config/
```

Compose 又把不同服务的外置目录挂载到容器的 `/app/config`：

```yaml
volumes:
  - ./config/provider:/app/config:ro
```

因此 `/app/config/application-vm.yaml` 会作为外置配置加载。`SPRING_PROFILE=dev,vm` 时，`application-vm.yaml` 用于覆盖内置 `dev` 配置中的基础设施地址。

`:ro` 表示容器内进程不能修改这个目录，但宿主机上的 Ubuntu 用户仍然可以编辑文件。

### 7.2 修改配置

推荐流程是修改仓库中的文件：

```text
deploy/config/<module>/application-vm.yaml
```

然后重新传输并重启对应服务：

```powershell
.\scripts\deploy.ps1
```

```bash
cd /home/zjc/zjc-app
docker compose restart service-provider
```

如果只在 Ubuntu 上临时修改，可以直接编辑：

```bash
vi /home/zjc/zjc-app/config/provider/application-vm.yaml
docker compose restart service-provider
```

但要注意：下次执行部署脚本时，本地仓库中的同名外置配置会重新传输到远程并覆盖它。长期配置应该改在 Git 仓库里，临时救火才直接改远程。

### 7.3 只写差异，不要复制全量配置

外置配置建议只写当前环境需要覆盖的属性，例如：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.100.128:8848
  datasource:
    url: jdbc:mysql://192.168.100.128:3306/spring_cloud_alibaba?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&sslMode=REQUIRED
  data:
    redis:
      host: 192.168.100.128
      port: 6379
```

没有写在 `application-vm.yaml` 里的配置，继续使用镜像内置配置。这样镜像内的业务配置和外置的环境地址职责清晰，也更容易确认差异。

### 7.4 迁移基础设施

如果 MySQL、Redis、Nacos、Zipkin 从 192.168.100.128 迁到其他机器，只需要修改相关 `application-vm.yaml` 中的地址，然后重新传输并重启服务，不需要重新构建镜像。

示例：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 10.0.0.8:8848
  datasource:
    url: jdbc:mysql://10.0.0.9:3306/spring_cloud_alibaba?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&sslMode=REQUIRED
  data:
    redis:
      host: 10.0.0.10
      port: 6379
```

前提是新基础设施允许 Docker 容器所在网段访问。

当前 `docker-compose.yml` 只包含业务容器，不包含 MySQL、Redis、Nacos 等基础设施。以后如果把基础设施也加入同一个 Compose，应用配置中的地址要改成 Compose 服务名，例如 `mysql:3306`、`redis:6379`、`nacos:8848`，并保证它们和业务服务在同一个网络中。

### 7.5 在其他环境指定外置配置

镜像天然支持 `/app/config`，这是推荐约定。如果临时用 `docker run` 启动，可以这样挂载：

```bash
docker run --rm \
  --network zjc-spring-cloud_zjc-net \
  -e SPRING_PROFILES_ACTIVE=dev,vm \
  -e JASYPT_ENCRYPTOR_PASSWORD="$JASYPT_ENCRYPTOR_PASSWORD" \
  -v /home/zjc/zjc-app/config/provider:/app/config:ro \
  zjc/service-provider:1.0.0
```

执行前需要先在 Shell 中设置 `JASYPT_ENCRYPTOR_PASSWORD`，不要把真实值写进命令历史。

示例中的 `zjc-spring-cloud_zjc-net` 是 Compose 创建后的实际网络名。如果还没有执行过 `docker compose up -d`，需要先启动 Compose，或者先手动创建同名网络。

如果确实要使用其他目录，不要依赖 `SPRING_CONFIG_ADDITIONAL_LOCATION` 环境变量覆盖，因为镜像里已经通过 JVM 系统参数指定了 `/app/config`，不同属性源的优先级容易让结果不符合预期。更可靠的是追加 Spring Boot 启动参数：

```bash
docker run --rm \
  -v /path/to/config:/app/custom-config:ro \
  zjc/service-provider:1.0.0 \
  --spring.config.additional-location=optional:file:/app/custom-config/
```

注意：启动参数中的 `spring.config.additional-location` 会替换镜像里原有的值。如果希望两个目录都生效，要显式写成列表：

```bash
--spring.config.additional-location=optional:file:/app/config/,optional:file:/app/custom-config/
```

生产上建议保持统一的 `/app/config` 约定，减少排查成本。

## 8. 版本更新与回滚

### 8.1 发布新版本

Windows：

```powershell
mvn -Pdocker-tar "-Ddocker.tag=1.0.1" -DskipTests clean package
.\scripts\deploy.ps1 -Tag "1.0.1" -Load
```

Ubuntu：

```bash
cd /home/zjc/zjc-app
vi .env
```

修改：

```text
APP_TAG=1.0.1
```

然后：

```bash
docker compose up -d
docker compose ps
```

如果只是新增模块或修改 Compose，也要执行 `docker compose up -d`，让 Compose 按新定义创建容器。

### 8.2 回滚

只要旧镜像还保留在部署机上：

```bash
cd /home/zjc/zjc-app
vi .env
```

把 `APP_TAG` 改回旧版本，例如：

```text
APP_TAG=1.0.0
```

然后：

```bash
docker compose up -d
```

### 8.3 清理旧镜像

查看镜像：

```bash
docker images "zjc/service-*"
```

确认不再需要后删除指定镜像：

```bash
docker rmi zjc/service-provider:1.0.0
```

清理悬空镜像：

```bash
docker image prune
```

不要在没有确认版本和回滚需求前使用 `docker system prune -a`，它会删除所有未被容器使用的镜像。

## 9. 一个镜像起多个服务实例

这里要分两种情况。

### 9.1 同一个镜像起多个实例：支持

例如 provider 起两个实例：

```bash
cd /home/zjc/zjc-app
docker compose up -d --scale service-provider=2
docker compose ps
```

两个 provider 容器都在 Docker 内部网络中，各自有独立 IP，容器内都监听 9001，不会互相冲突。它们会注册到 Nacos，gateway 通过 Nacos 获得两个实例并负载均衡访问。

缩回一个实例：

```bash
docker compose up -d --scale service-provider=1
```

注意事项：

- 适合无状态服务。provider、consumer、gateway 这类服务更适合扩容。
- 多实例可以共用同一个镜像和同一个容器端口，因为每个容器有独立网络命名空间。
- 不要直接 `--scale service-gateway=2`，当前 gateway 固定映射宿主机 80 端口，两个实例会争抢同一个宿主机端口。
- 当前文件日志按服务名和日期滚动，没有按容器实例隔离。多实例长期运行前，建议把 Logback 日志路径或文件名加上容器 `HOSTNAME`，否则多个 JVM 可能写同一组宿主机日志文件。
- 如果服务有本地文件状态、定时任务抢占、顺序消息等问题，扩容前要单独设计，不能只看 HTTP 请求是否负载均衡。

gateway 需要多实例时，通常做法是：

1. 多个 gateway 只在 Docker 内部监听 80。
2. 前面再加 Nginx、云负载均衡或宿主机端口负载均衡。
3. 由外部负载均衡统一暴露一个入口。

### 9.2 一个镜像运行多个不同服务：不建议

当前设计是一个可运行模块一个镜像：

```text
zjc/service-provider -> ProviderApplication
zjc/service-consumer -> ConsumerApplication
zjc/service-gateway  -> GatewayApplication
zjc/service-mail     -> MailApplication
```

理论上可以把多个启动类和依赖打进一个大镜像，再通过不同启动参数运行不同 main class，但当前不建议：

- 镜像会越来越大，任何一个服务改代码都可能导致所有服务重新发布。
- 依赖冲突会集中到同一个 classpath。
- 启动类、端口、服务名、配置都要额外覆盖，排错复杂。
- 构建缓存和发布粒度变差。
- 一个服务漏洞升级会迫使其他服务一起重建镜像。

推荐原则：

```text
一个可运行服务一个镜像；
同一个镜像可以起多个实例；
不同服务使用不同镜像。
```

## 10. 新增一个模块

以 `service-ai` 为例，端口假设为 9005。

### 10.1 根 POM 注册模块

修改根 `pom.xml`：

```xml
<modules>
    <module>service-common</module>
    <module>service-provider</module>
    <module>service-consumer</module>
    <module>service-gateway</module>
    <module>service-mail</module>
    <module>service-ai</module>
</modules>
```

### 10.2 模块 POM 增加 docker-tar Profile

在 `service-ai/pom.xml` 中加入：

```xml
<profiles>
    <profile>
        <id>docker-tar</id>
        <properties>
            <docker.main-class>com.zjc.ai.AiApplication</docker.main-class>
            <docker.container-port>9005</docker.container-port>
            <jib.skip>false</jib.skip>
        </properties>
    </profile>
</profiles>
```

`docker.main-class` 必须是真实启动类，`docker.container-port` 必须与该模块 `application.yaml` 中的 `server.port` 一致。

模块目录和 Maven `artifactId` 建议保持 `service-ai`。部署脚本会根据 Compose 里的 `zjc/service-ai` 推导 `service-ai\target\jib-image.tar`，目录名、artifactId 和镜像名不一致时脚本无法自动找到产物。

### 10.3 编写模块配置

按现有服务结构创建：

```text
service-ai/src/main/resources/application.yaml
service-ai/src/main/resources/application-dev.yaml
service-ai/src/main/resources/application-prod.yaml
service-ai/src/main/resources/config/application-nacos.yaml
service-ai/src/main/resources/config/application-shutdown.yaml
```

基础内容参考现有模块：

```yaml
server:
  port: 9005

spring:
  application:
    name: service-ai
  profiles:
    active: dev
    include:
      - api
      - jasypt
      - zipkin
      - nacos
      - shutdown
```

`spring.application.name` 会作为 Nacos 服务名，也会影响日志目录和缓存前缀，不要随便改。

### 10.4 Compose 增加服务

在 `deploy/docker-compose.yml` 的 `services` 下增加：

```yaml
  service-ai:
    image: zjc/service-ai:${APP_TAG}
    environment: *common-environment
    expose:
      - "9005"
    networks:
      - zjc-net
    volumes:
      - ./config/ai:/app/config:ro
      - ./logs/ai:/app/logs/service-ai
    restart: unless-stopped
    stop_grace_period: 40s
```

如果希望 gateway 等 AI 就绪后再启动，还可以把 `service-ai` 加进 gateway 的 `depends_on`。不过 `depends_on` 只控制容器启动顺序，不代表 AI 已经完成 Nacos 注册。

### 10.5 增加外置配置

创建：

```text
deploy/config/ai/application-vm.yaml
```

示例：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.100.128:8848

management:
  tracing:
    export:
      zipkin:
        endpoint: http://192.168.100.128:9411/api/v2/spans
```

如果 AI 也使用 MySQL 或 Redis，在这个文件里继续覆盖对应地址。

### 10.6 构建和部署

部署脚本会自动发现 Compose 中的 `zjc/service-ai`，不需要修改脚本：

```powershell
mvn -Pdocker-tar "-Ddocker.tag=1.0.1" -DskipTests clean package
.\scripts\deploy.ps1 -Tag "1.0.1" -Load
```

Ubuntu：

```bash
cd /home/zjc/zjc-app
docker compose up -d
docker compose logs -f service-ai
```

### 10.7 按业务需要补充

以下不是每个新模块都需要：

- 外部要通过网关访问 AI：给 gateway 增加 `service-ai` 路由。
- consumer 要调用 AI：新增 Feign API 和 fallback。
- AI 要被其他服务调用：注册到 Nacos 后，调用方按服务名访问。
- AI 只做后台任务：可以不暴露网关路由，但仍建议接入 Nacos、日志、追踪和优雅停机。
- 需要接口文档：补充 Swagger 聚合配置。

## 11. 常见问题和踩坑点

### 11.1 容器里不能用 127.0.0.1 访问宿主机基础设施

错误示例：

```yaml
url: jdbc:mysql://127.0.0.1:3306/...
```

容器里的 `127.0.0.1` 指容器自己，不是 Ubuntu 宿主机。当前基础设施在宿主机网络中，所以使用：

```text
192.168.100.128
```

如果以后 MySQL、Redis、Nacos 搬到其他机器，就改成对应机器 IP。

### 11.2 Nacos 注册的是容器 IP

provider、consumer、mail、gateway 必须在同一个 Docker 网络中，当前是 `zjc-net`。否则可能出现：

```text
服务已经在 Nacos 有实例，gateway 却访问不到
```

原因是 Nacos 注册的是容器网络 IP，gateway 只有在同一个网络里才能访问这个 IP。

如果以后服务分布在多台 Ubuntu 机器上，默认单机 bridge 网络就不适用，需要重新设计，例如 overlay network、Kubernetes、host network，或者让服务注册可路由的宿主机 IP。

### 11.3 APP_TAG 和 docker load 不一致

典型现象：

```text
pull access denied for zjc/service-provider
```

或 Compose 找不到镜像。

原因是 `.env` 里的 `APP_TAG` 指向一个没有被 `docker load` 过的 tag。检查：

```bash
docker images "zjc/service-*"
grep APP_TAG .env
```

必须保持：

```text
Windows 构建 tag = 部署机已加载 tag = .env APP_TAG
```

### 11.4 修改配置后没有重启

Spring Boot 配置在启动时读取。修改 `application-vm.yaml` 后必须重启对应服务：

```bash
docker compose restart service-provider
```

如果修改的是 `docker-compose.yml`，使用：

```bash
docker compose up -d
```

### 11.5 外置配置路径或 profile 不匹配

检查三点：

```text
1. 文件是否挂载到了 /app/config
2. SPRING_PROFILES_ACTIVE 是否包含 vm
3. 文件名是否是 application-vm.yaml
```

进入容器检查：

```bash
docker compose exec service-provider sh
ls -l /app/config
```

### 11.6 Jasypt 主密钥缺失或错误

现象通常是应用启动失败，配置中的 `ENC(...)` 无法解密。

检查：

```bash
grep JASYPT_ENCRYPTOR_PASSWORD .env
```

所有使用加密配置的服务必须拿到同一个主密钥。不要把真实值提交到 Git。

### 11.7 基础设施只监听 127.0.0.1

如果 MySQL 配置成只监听 `127.0.0.1`，或者 Redis、Nacos、防火墙不允许 Docker 网段访问，容器会连接失败。

排查思路：

```bash
# 在 Ubuntu 宿主机看监听地址
ss -lntp | grep -E "3306|6379|8848|9411|1025"

# 进入容器网络测试
docker compose exec service-provider sh
```

如果服务只绑定在 `127.0.0.1`，需要改成 `0.0.0.0`，并通过防火墙限制来源网段。

### 11.8 Windows 脚本执行策略

如果提示 PowerShell 脚本不能执行，使用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -Build
```

这只是对当前进程放开限制，不会修改系统全局执行策略。

### 11.9 depends_on 不等于服务就绪

当前 gateway 的 `depends_on` 只保证容器启动顺序，不保证 provider 已经完成 Spring 启动和 Nacos 注册。应用刚启动后短暂 404、503 或服务列表为空，通常等待几秒后会恢复。

### 11.10 镜像架构不匹配

如果运行时报：

```text
exec format error
```

常见原因是镜像架构和 Ubuntu CPU 架构不一致。当前镜像固定为 `linux/amd64`。

检查：

```bash
uname -m
docker image inspect zjc/service-provider:1.0.0 --format '{{.Architecture}}'
```

### 11.11 基础镜像拉取失败

Jib 构建时需要读取基础镜像 manifest。如果 `dockerproxy.net` 不可用，可以换基础镜像源：

```powershell
mvn -Pdocker-tar "-Ddocker.base-image=<可用的 eclipse-temurin:21-jre 地址>" "-Ddocker.tag=1.0.0" -DskipTests clean package
```

如果长期更换，建议直接修改根 POM 中的 `docker.base-image`。

### 11.12 磁盘空间

每个版本会同时留下：

```text
Windows 镜像 tar
Ubuntu 镜像 tar
Ubuntu 已加载镜像
宿主机日志
```

定期检查：

```bash
df -h
docker system df
du -sh /home/zjc/zjc-app/logs/*
```

日志已有保留策略，但磁盘仍然需要监控。

### 11.13 远程残留旧配置

部署脚本使用 `scp -r` 覆盖同名文件，但不会删除远程已经不存在于本地仓库的旧文件。如果删除了某个模块或某个外置配置文件，需要手动清理远程对应文件。

当前本地目录已经命名为 `deploy/config`。如果部署机以前部署过旧版本并存在 `/home/zjc/zjc-app/external-config`，新版本不会再使用它。确认 `config` 目录和服务运行正常后，可以手动清理旧目录：

```bash
ls -la /home/zjc/zjc-app/config
rm -rf /home/zjc/zjc-app/external-config
```

注意：已有容器仍保留着创建时的旧挂载路径。传输新 Compose 后需要在 Ubuntu 执行 `docker compose up -d` 重建容器，新的 `./config/...` 挂载才会生效。

### 11.14 不要把基础设施做成镜像的一部分

镜像应该保持环境无关。基础设施地址、密钥、外部资源地址放入 `.env` 或外置配置，不要硬编码进业务镜像。

## 12. 日常维护手册

以下命令默认都在 Ubuntu 部署机执行：

```bash
cd /home/zjc/zjc-app
```

### 12.1 启动、停止和重启

启动全部服务：

```bash
docker compose up -d
```

启动已经存在但处于停止状态的容器：

```bash
docker compose start
```

停止全部服务：

```bash
docker compose stop
```

停止后再次启动：

```bash
docker compose stop
docker compose start
```

重启全部服务：

```bash
docker compose restart
```

只重启一个服务：

```bash
docker compose restart service-provider
docker compose restart service-gateway
```

只停止一个服务：

```bash
docker compose stop service-provider
```

如果之前执行过 `--scale service-provider=2`，按服务名停止或重启时会作用于该服务的所有实例。

停止并删除容器：

```bash
docker compose down
```

清理不属于当前 Compose 文件的孤儿容器：

```bash
docker compose down --remove-orphans
```

几个命令的区别：

| 命令 | 容器是否删除 | 适用场景 |
| --- | --- | --- |
| `docker compose start` | 否 | 启动已停止的容器 |
| `docker compose stop` | 否 | 临时停机、日常维护 |
| `docker compose restart` | 否 | 修改外置 YAML 后让应用重新读取配置 |
| `docker compose up -d` | 按需创建或重建 | 镜像版本、`.env`、Compose 文件变化后 |
| `docker compose down` | 是 | 清理容器和网络，但保留镜像、配置、日志 |

注意：`.env` 中的变量是在容器创建时注入的。修改 `.env` 后执行 `docker compose restart` 不一定重建容器，正确做法是：

```bash
docker compose up -d
```

### 12.2 发布新版本

完整发布流程在 Windows 上执行：

```powershell
mvn -Pdocker-tar "-Ddocker.tag=1.0.1" -DskipTests clean package
.\scripts\deploy.ps1 -Tag "1.0.1" -Load
```

然后在 Ubuntu 上修改 `.env`：

```bash
cd /home/zjc/zjc-app
vi .env
```

确认：

```text
APP_TAG=1.0.1
```

重建并启动容器：

```bash
docker compose up -d
docker compose ps
```

发布后检查：

```bash
docker compose ps
docker compose logs --tail=200 service-provider
curl http://192.168.100.128/api/v1/provider/user/1
curl http://192.168.100.128/api/v1/consumer/user/1
```

回滚只需要把 `.env` 中的 `APP_TAG` 改回旧版本，并确认旧镜像仍在部署机上：

```bash
docker images "zjc/service-*"
docker compose up -d
```

### 12.3 修改配置

外置配置的长期维护入口在仓库中：

```text
deploy/config/<module>/application-vm.yaml
```

推荐流程：

```powershell
# Windows：传输新的外置配置
.\scripts\deploy.ps1
```

```bash
# Ubuntu：重启对应服务
cd /home/zjc/zjc-app
docker compose restart service-provider
```

只修改 `.env` 时的流程：

```bash
cd /home/zjc/zjc-app
vi .env
docker compose up -d
```

只修改 `docker-compose.yml` 时的流程：

```bash
cd /home/zjc/zjc-app
docker compose up -d
```

如果只是在 Ubuntu 上临时改外置 YAML，可以直接编辑并重启：

```bash
vi /home/zjc/zjc-app/config/provider/application-vm.yaml
docker compose restart service-provider
```

但要记住：下次执行部署脚本时，仓库中的同名文件会覆盖远程临时修改。长期配置必须提交回仓库。

### 12.4 扩容和缩容

provider 扩到两个实例：

```bash
docker compose up -d --scale service-provider=2
docker compose ps
```

provider 缩回一个实例：

```bash
docker compose up -d --scale service-provider=1
```

扩容后确认：

```bash
docker compose ps
docker compose logs -f service-provider
```

多请求几次网关，观察请求是否分散到不同 provider 实例：

```bash
curl http://192.168.100.128/api/v1/provider/user/1
```

不要直接扩容 gateway：

```bash
docker compose up -d --scale service-gateway=2
```

当前 gateway 固定映射宿主机 80 端口，两个实例会争抢同一个宿主机端口。gateway 多实例需要先去掉直接 `ports` 映射，再增加 Nginx 或其他负载均衡入口。

### 12.5 日志维护

容器 stdout/stderr 日志：

```bash
docker compose logs -f
docker compose logs -f service-provider
docker compose logs --tail=200 service-gateway
```

单个容器日志：

```bash
docker ps
docker logs -f zjc-spring-cloud-service-provider-1
```

应用文件日志：

```text
/home/zjc/zjc-app/logs/provider/service-provider/info/service-provider-info-YYYY-MM-DD.N.log
/home/zjc/zjc-app/logs/consumer/service-consumer/info/service-consumer-info-YYYY-MM-DD.N.log
/home/zjc/zjc-app/logs/gateway/service-gateway/info/service-gateway-info-YYYY-MM-DD.N.log
/home/zjc/zjc-app/logs/mail/service-mail/info/service-mail-info-YYYY-MM-DD.N.log
```

debug、warn、error 分别在对应级别目录下：

```text
logs/provider/service-provider/debug/
logs/provider/service-provider/info/
logs/provider/service-provider/warn/
logs/provider/service-provider/error/
```

查看当前实际日志文件：

```bash
find /home/zjc/zjc-app/logs -type f -name "*.log" -ls
```

应用文件日志由 Logback 控制：

```text
单文件 1MB
按日期和大小滚动
保留 180 天
每级别总量 2GB
```

Docker 的 json-file 日志当前没有在 Compose 中配置轮转上限。如果长期运行，建议给每个服务增加：

```yaml
logging:
  driver: json-file
  options:
    max-size: "50m"
    max-file: "5"
```

### 12.6 日常巡检

检查容器状态：

```bash
docker compose ps
```

重点看：

```text
State 是否 Up
Status 是否 healthy 或无异常退出信息
gateway 是否映射 80:80
provider 副本数是否符合预期
```

检查资源：

```bash
docker stats
df -h
docker system df
```

检查业务入口：

```bash
curl http://192.168.100.128/api/v1/provider/user/1
curl http://192.168.100.128/api/v1/consumer/user/1
```

检查 Compose 渲染结果：

```bash
docker compose config
```

检查容器环境变量：

```bash
docker compose exec service-provider env | sort
```

检查外置配置是否挂载：

```bash
docker compose exec service-provider ls -l /app/config
```

### 12.7 清理磁盘

先检查空间：

```bash
df -h
docker system df
du -sh /home/zjc/zjc-app/logs/*
du -sh /home/zjc/zjc-app/images/*
```

查看业务镜像：

```bash
docker images "zjc/service-*"
```

删除确认不再需要的旧镜像：

```bash
docker rmi zjc/service-provider:1.0.0
```

清理悬空镜像：

```bash
docker image prune
```

清理不再需要的镜像 tar：

```bash
ls -lh /home/zjc/zjc-app/images
rm /home/zjc/zjc-app/images/service-provider.tar
```

不要盲目执行：

```bash
docker system prune -a
```

它会删除所有未被容器使用的镜像，可能把回滚版本一起删掉。

### 12.8 备份和保护范围

当前部署目录中必须备份或保护的内容：

```text
/home/zjc/zjc-app/.env
/home/zjc/zjc-app/docker-compose.yml
/home/zjc/zjc-app/config/
/home/zjc/zjc-app/images/
/home/zjc/zjc-app/logs/
```

其中：

- `.env` 包含 Jasypt 主密钥，不能提交 Git，也不能随意截图或输出。
- `docker-compose.yml` 和 `config` 的源头在 Git 仓库中，优先维护仓库版本。
- `images` 可以按版本保留，用于快速回滚。
- `logs` 按审计和排障要求归档。

MySQL、Redis、Nacos、Zipkin、Mailhog 当前不在这个 Compose 内，它们的数据备份需要在对应基础设施侧单独做。不要以为备份了业务容器就等于备份了数据库和 Nacos 数据。

### 12.9 维护前后的检查清单

维护前：

```bash
docker compose ps
docker images "zjc/service-*"
df -h
```

确认：

- 当前运行版本和 `.env` 的 `APP_TAG`。
- 旧镜像是否还在，能否回滚。
- 磁盘空间是否足够。
- 本次维护涉及哪些服务。

维护后：

```bash
docker compose ps
docker compose logs --tail=200 service-provider
curl http://192.168.100.128/api/v1/provider/user/1
```

确认：

- 容器没有重启循环。
- Nacos 上实例状态正常。
- 网关接口可用。
- 错误日志没有新增异常。

### 12.10 常用命令速查

```bash
cd /home/zjc/zjc-app

# 状态
docker compose ps

# 启动、停止、重启
docker compose up -d
docker compose start
docker compose stop
docker compose restart service-provider
docker compose down

# 日志
docker compose logs -f service-gateway
docker compose logs --tail=200 service-provider

# 扩缩容
docker compose up -d --scale service-provider=2
docker compose up -d --scale service-provider=1

# 进入容器
docker compose exec service-provider sh

# 查看环境变量
docker compose exec service-provider env | sort

# 查看外置配置
docker compose exec service-provider ls -l /app/config

# 镜像
docker images "zjc/service-*"

# Compose 配置渲染结果
docker compose config
```
