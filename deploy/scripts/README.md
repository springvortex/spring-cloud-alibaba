# 部署脚本使用说明

本文只说明部署相关脚本。镜像构建、Docker 安装、外置配置、扩缩容、回滚和日志的完整说明见
[../README.md](../README.md)。

## 脚本分工

```text
deploy/scripts/windows/deploy.ps1
  运行位置：Windows 开发机
  职责：构建/检查/传输镜像 tar、Compose、配置模板，并可选远程加载和启动

deploy/scripts/unix/manage.sh
  运行位置：Ubuntu/macOS 部署机
  职责：只维护已经在部署目录里的 Docker Compose 应用，不负责构建和传输
```

两个脚本配合后的典型流程是：

```text
Windows 开发机执行 deploy.ps1
  -> 生成/传输镜像 tar 和部署文件
  -> Ubuntu 执行 manage.sh
  -> 加载镜像、启动服务、查看日志、扩缩容、重启配置
```

## 1. Windows 部署脚本

脚本路径：

```text
deploy/scripts/windows/deploy.ps1
```

### 1.1 前置条件

Windows 开发机需要：

- JDK 21
- Maven
- `ssh` / `scp`
- 能通过 SSH 登录目标 Ubuntu 服务器

`Build` 模式执行 Jib `buildTar`，不需要 Windows 安装 Docker。`Load` 和 `Start` 模式只是通过 SSH
调用远程 Docker，因此 Docker 只需要安装在 Ubuntu 服务器上。

### 1.2 默认目标

```text
Remote  = zjc@192.168.100.128
AppDir  = /home/zjc/app
Tag     = 1.0.0
```

### 1.3 参数

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `-Remote` | `zjc@192.168.100.128` | SSH 目标，格式必须是 `user@host` |
| `-AppDir` | `/home/zjc/app` | Ubuntu 部署目录 |
| `-Tag` | `1.0.0` | 构建和传输的镜像 tag；远程 `.env` 的 `APP_TAG` 必须一致 |
| `-Build` | 关闭 | 先执行 `mvn -Pdocker-tar clean package`，再传输 |
| `-Load` | 关闭 | 传输后在远程执行 `docker load` |
| `-Start` | 关闭 | 远程加载镜像后执行 `docker compose up -d` |
| `-MavenCommand` | `mvn` | Maven 可执行命令，适合 Maven 不在 PATH 或路径带空格时使用 |

`Start` 自动包含 `Load` 动作；不会自动创建或修改远程 `.env`。

### 1.4 常用命令

在仓库根目录执行：

```powershell
# 只传输已经存在的镜像 tar 和部署文件
.\deploy\scripts\windows\deploy.ps1

# 构建后传输
.\deploy\scripts\windows\deploy.ps1 -Build

# 构建后传输，并在远程加载镜像
.\deploy\scripts\windows\deploy.ps1 -Build -Load

# 构建、传输、远程加载并启动
.\deploy\scripts\windows\deploy.ps1 -Build -Start

# 自定义目标、目录和镜像 tag
.\deploy\scripts\windows\deploy.ps1 `
  -Remote "zjc@192.168.100.128" `
  -AppDir "/home/zjc/app" `
  -Tag "1.0.1" `
  -Build -Start
```

如果 PowerShell 执行策略拦截脚本，可以使用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\scripts\windows\deploy.ps1 -Build
```

### 1.5 脚本会传输哪些内容

Windows 脚本会同步：

```text
deploy/docker-compose.yml
deploy/.env.example
deploy/config/<module>/application-vm.yaml.template
deploy/README.md
deploy/scripts/README.md
deploy/scripts/unix/manage.sh
service-*/target/jib-image.tar
```

远程落盘位置是：

```text
/home/zjc/app/docker-compose.yml
/home/zjc/app/.env.example
/home/zjc/app/config/<module>/application-vm.yaml.template
/home/zjc/app/README.md
/home/zjc/app/scripts/README.md
/home/zjc/app/scripts/unix/manage.sh
/home/zjc/app/images/service-<module>.tar
```

模块列表从 `deploy/docker-compose.yml` 的 `zjc/service-*:${APP_TAG}` 镜像定义解析。
以后新增 `service-ai` 时，只要 Compose 中有 `zjc/service-ai:${APP_TAG}`，并且存在：

```text
deploy/config/ai/application-vm.yaml.template
service-ai/target/jib-image.tar
```

脚本会自动检查并传输，不需要修改 `deploy.ps1`。

远程镜像 tar 固定命名为：

```text
/home/zjc/app/images/service-<module>.tar
```

新版本会覆盖同名的旧 tar。Docker daemon 中的旧 tag 镜像是否保留，取决于是否执行过 `docker rmi`。

配置模板也会被覆盖更新，但远程实际运行的
`config/<module>/application-vm.yaml` 不会被覆盖。该文件首次需要在 Ubuntu 手动执行：

```bash
cd /home/zjc/app
for module in provider consumer gateway mail; do
  cp "config/$module/application-vm.yaml.template" \
     "config/$module/application-vm.yaml"
done
```

### 1.6 Windows 脚本不会做的事

- 不创建、不读取明文、不覆盖远程 `.env`，避免误删 Jasypt 主密钥。
- 不创建、不覆盖远程 `config/<module>/application-vm.yaml` 运行配置。
- 不自动修改 `.env` 中的 `APP_TAG`。
- 不做健康检查和自动回滚。
- 不清理远程旧镜像、旧 tar、历史日志。
- 不修改 Ubuntu 防火墙、Docker 安装和系统内核参数。

首次部署或更换 `Tag` 后，需要在 Ubuntu 检查 `.env`：

```bash
cd /home/zjc/app
vi .env
```

确保：

```text
APP_TAG=1.0.1
```

然后在 Ubuntu 执行 `docker compose up -d`，或使用 `manage.sh apply all` 重建容器。

## 2. Ubuntu/macOS 维护脚本

脚本路径：

```text
deploy/scripts/unix/manage.sh
```

Windows 部署脚本会把它同步到远程：

```text
/home/zjc/app/scripts/unix/manage.sh
```

### 2.1 前置条件

部署机需要：

- Docker Engine
- Docker Compose 插件，或旧版 `docker-compose` 命令
- 已存在部署目录：

```text
/home/zjc/app/docker-compose.yml
/home/zjc/app/.env
/home/zjc/app/config/
/home/zjc/app/images/*.tar
```

`.env` 首次需要从模板创建：

```bash
cd /home/zjc/app
cp .env.example .env
chmod 600 .env
vi .env
```

### 2.2 进入交互菜单

```bash
cd /home/zjc/app
./scripts/unix/manage.sh
```

菜单能力：

- 查看容器状态、服务列表、镜像和配置清单。
- 启动、停止、重启全部服务或指定服务。
- 外置 YAML 修改后重启指定服务。
- `.env`、镜像 tag、Compose 文件变更后应用新定义。
- 停止并删除指定容器或全部容器。
- 跟踪全部或指定服务日志，查看最近日志。
- 加载已上传的镜像 tar。
- 扩缩容指定服务。
- 校验 Compose 配置。
- 编辑 `.env`。
- 按 `0` 退出。

菜单中的启动、停止、重启默认保留容器，不会删除容器。删除类操作会先要求确认。

### 2.3 命令行模式

不进入菜单时，可以直接执行命令：

```bash
./scripts/unix/manage.sh status
./scripts/unix/manage.sh services
./scripts/unix/manage.sh start all
./scripts/unix/manage.sh stop service-provider
./scripts/unix/manage.sh restart service-provider
./scripts/unix/manage.sh config-restart service-provider
./scripts/unix/manage.sh apply all
./scripts/unix/manage.sh rm service-provider
./scripts/unix/manage.sh down
./scripts/unix/manage.sh logs service-provider
./scripts/unix/manage.sh recent service-provider 300
./scripts/unix/manage.sh load
./scripts/unix/manage.sh scale service-provider 2
./scripts/unix/manage.sh images
./scripts/unix/manage.sh validate
./scripts/unix/manage.sh info
```

服务名可以写多个，也可以用逗号分隔：

```bash
./scripts/unix/manage.sh restart service-provider,service-gateway
```

`all` 表示全部服务。服务名支持使用菜单展示的编号，但脚本执行时建议写名称，便于放入运维记录。

### 2.4 常用选项

| 选项 | 说明 |
| --- | --- |
| `--app-dir <dir>` | 指定部署目录；默认当前目录 |
| `-y` / `--yes` | 跳过危险操作确认，仅建议自动化场景使用 |
| `-h` / `--help` | 查看脚本帮助 |

示例：

```bash
./scripts/unix/manage.sh --app-dir /home/zjc/app status
./scripts/unix/manage.sh --app-dir /home/zjc/app -y down
```

也支持环境变量：

```bash
ZJC_APP_DIR=/home/zjc/app ./scripts/unix/manage.sh status
```

### 2.5 命令选择指南

| 场景 | 推荐命令 |
| --- | --- |
| 临时停止服务，稍后继续启动 | `stop`，之后 `start` |
| 修改 `config/<module>/application-vm.yaml` | `config-restart <service>` |
| 修改 `.env`、`APP_TAG`、`docker-compose.yml` | `apply all` |
| 加载新的 `images/*.tar` | `load`，然后 `apply all` |
| provider 临时扩两个实例 | `scale service-provider 2` |
| 清理容器和网络但保留数据 | `down` |
| 查看请求是否进入容器 | `logs <service>` 或 `recent <service>` |

`restart` 只重启已有容器，不会让 `.env` 和新镜像生效。涉及镜像 tag 或环境变量时必须使用
`apply`，必要时 Compose 会重建容器。

### 2.6 删除操作的范围

`rm <service...>`：

- 停止并删除指定服务的容器。
- 保留镜像、`.env`、外置配置、镜像 tar 和应用文件日志。

`down`：

- 停止并删除全部容器。
- 删除 Compose 创建的网络。
- 保留镜像、`.env`、外置配置、镜像 tar 和应用文件日志。
- 下次 `start` 前需要执行 `apply` 或 `docker compose up -d` 重新创建容器。

不会自动删除的内容：

```text
/home/zjc/app/.env
/home/zjc/app/config/
/home/zjc/app/images/
/home/zjc/app/logs/
Docker daemon 中的镜像
```

## 3. 发布新版本

### 3.1 构建、传输并加载

Windows：

```powershell
.\deploy\scripts\windows\deploy.ps1 -Build -Load -Tag "1.0.1"
```

Ubuntu：

```bash
cd /home/zjc/app
vi .env
```

把 `APP_TAG` 改成：

```text
APP_TAG=1.0.1
```

应用新镜像：

```bash
./scripts/unix/manage.sh apply all
./scripts/unix/manage.sh status
```

### 3.2 验证

```bash
./scripts/unix/manage.sh images
./scripts/unix/manage.sh logs service-provider
curl http://192.168.100.128/api/v1/provider/user/1
curl http://192.168.100.128/api/v1/consumer/user/1
```

确认新版本稳定后，再考虑清理旧镜像：

```bash
docker images "zjc/service-*"
docker rmi zjc/service-provider:1.0.0
```

## 4. 常见问题

### 4.1 镜像加载了，但容器还是旧版本

通常是 `.env` 中的 `APP_TAG` 没变，或只执行了 `restart`。正确流程：

```bash
cd /home/zjc/app
grep APP_TAG .env
./scripts/unix/manage.sh apply all
```

### 4.2 修改外置配置后没有生效

外置 YAML 是应用启动时读取的，修改后需要重启：

```bash
./scripts/unix/manage.sh config-restart service-provider
```

如果修改的是 `.env` 或 Compose 文件，则必须使用：

```bash
./scripts/unix/manage.sh apply all
```

### 4.3 gateway 不能通过脚本扩多个实例

当前 Compose 中 gateway 固定映射宿主机 `80` 端口。直接扩两个 gateway 会争抢同一个端口。
脚本会拦截 `service-gateway` 扩到多实例的操作。若要多入口，需要先调整 Compose 并引入外部负载均衡。

### 4.4 `manage.sh` 没有可执行权限

如果手动复制过脚本，可以执行：

```bash
chmod 700 /home/zjc/app/scripts/unix/manage.sh
```

Windows 部署脚本同步时会自动设置该权限。

### 4.5 Windows 找不到 Maven

确认 Maven 在 PATH 中：

```powershell
mvn -v
```

如果 Maven 命令路径特殊，可显式传入：

```powershell
.\deploy\scripts\windows\deploy.ps1 -Build -MavenCommand "D:\tools\maven\bin\mvn.cmd"
```

### 4.6 ARM64 Ubuntu

默认 Jib 配置生成 `linux/amd64` 镜像。Ubuntu 输出 `aarch64` 时，需要先按部署手册修改 Jib
架构配置，再重新构建和传输；否则容器启动会报 `exec format error`。

## 5. 安全和版本注意事项

- `.env` 和服务器上的 `application-vm.yaml` 运行配置只保存在部署机，不提交 Git。
- 不要把 Jasypt 主密钥写入脚本、README 或终端截图。
- Windows 脚本只覆盖配置模板；服务器上的 `application-vm.yaml` 属于运行配置，不会被脚本覆盖。模板默认值和运行配置的差异需要人工同步。
- 新镜像 tag 发布后，旧镜像是回滚依据；确认稳定后再删除。
- 不要把 `/home/zjc/app/logs`、`.env`、运行配置或镜像 tar 加入 Git。
