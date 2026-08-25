#Requires -Version 5.1

<#
.SYNOPSIS
    构建 Jib 镜像 tar，并把应用包传输到 VMware Ubuntu 部署机。

.DESCRIPTION
    默认行为：
      1. 可选执行 Maven 构建生成四个 target/jib-image.tar。
      2. 检查 Compose、外置配置和镜像 tar 是否齐全。
      3. 通过 ssh/scp 传输到远程 AppDir。

    传输后默认不加载镜像、不启动服务。需要继续执行远程操作时：
      -Load  : 在 Ubuntu 上执行 docker load。
      -Start : 执行 docker load 后启动 docker compose。

.EXAMPLE
    # 只传输已有镜像 tar 和部署文件
    .\scripts\deploy.ps1

.EXAMPLE
    # 先重新构建，再传输
    .\scripts\deploy.ps1 -Build

.EXAMPLE
    # 构建、传输、远程加载并启动
    .\scripts\deploy.ps1 -Build -Start

.EXAMPLE
    # 指定远程用户、主机、目录和镜像 tag
    .\scripts\deploy.ps1 -Remote "zjc@192.168.100.128" -AppDir "/home/zjc/zjc-app" -Tag "1.0.0"
#>
[CmdletBinding()]
param(
    # SSH 目标，格式必须是 user@host。
    [ValidateNotNullOrEmpty()]
    [string]$Remote = "zjc@192.168.100.128",

    # Ubuntu 上的部署目录；部署文件会复制到这个目录下。
    [ValidateNotNullOrEmpty()]
    [string]$AppDir = "/home/zjc/zjc-app",

    # Maven 构建 tag。自定义 tag 后，远程 .env 的 APP_TAG 也要改成同一个值。
    [ValidateNotNullOrEmpty()]
    [string]$Tag = "1.0.0",

    # 构建前先执行 mvn -Pdocker-tar clean package。
    [switch]$Build,

    # 传输后在远程执行 docker load。
    [switch]$Load,

    # 传输后加载镜像并执行 docker compose up -d；不会自动创建或修改 .env。
    [switch]$Start,

    # Maven 命令，路径中有空格时可用引号包裹。
    [string]$MavenCommand = "mvn"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

foreach ($name in @("ssh", "scp")) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "找不到命令：$name。请先安装 OpenSSH 客户端。"
    }
}

if ($Build -and -not (Get-Command $MavenCommand -ErrorAction SilentlyContinue)) {
    throw "找不到 Maven 命令：$MavenCommand"
}

Push-Location $repositoryRoot
try {
    # 从 Compose 的 image: zjc/service-xxx:tag 中解析模块，后续新增服务不需要修改本脚本。
    $modules = @(
        Get-Content -LiteralPath "deploy\docker-compose.yml" |
            ForEach-Object {
                if ($_ -match 'image:\s*zjc/service-(?<module>[a-z0-9-]+):') {
                    $Matches["module"]
                }
            }
    )
    if ($modules.Count -eq 0) {
        throw "deploy\docker-compose.yml 中没有找到 zjc/service-* 镜像定义。"
    }

    if ($Build) {
        Write-Host "==> 构建 linux/amd64 镜像 tar：$Tag" -ForegroundColor Cyan
        & $MavenCommand "-Pdocker-tar" "-Ddocker.tag=$Tag" "-DskipTests" "clean" "package"
        if ($LASTEXITCODE -ne 0) {
            throw "Maven 构建失败，退出码：$LASTEXITCODE"
        }
    }

    # 先做完整性检查，避免远程目录传到一半才发现缺文件。
    $requiredFiles = @(
        "deploy\docker-compose.yml",
        "deploy\.env.example"
    )
    foreach ($module in $modules) {
        $requiredFiles += "deploy\config\$module\application-vm.yaml"
        $requiredFiles += "service-$module\target\jib-image.tar"
    }

    $missingFiles = @($requiredFiles | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
    if ($missingFiles.Count -gt 0) {
        $missingList = ($missingFiles -join [Environment]::NewLine)
        throw "以下文件不存在，请先执行 .\scripts\deploy.ps1 -Build 或检查部署目录：$([Environment]::NewLine)$missingList"
    }

    Write-Host "==> 准备远程目录：${Remote}:$AppDir" -ForegroundColor Cyan
    & ssh $Remote "mkdir -p '$AppDir/images'"
    if ($LASTEXITCODE -ne 0) {
        throw "无法创建远程目录：$AppDir"
    }

    Write-Host "==> 传输 Compose 与外置配置" -ForegroundColor Cyan
    & scp "deploy\docker-compose.yml" "$($Remote):$AppDir/docker-compose.yml"
    if ($LASTEXITCODE -ne 0) {
        throw "传输 docker-compose.yml 失败"
    }

    & scp "deploy\.env.example" "$($Remote):$AppDir/.env.example"
    if ($LASTEXITCODE -ne 0) {
        throw "传输 .env.example 失败"
    }

    & scp -r "deploy\config" "$($Remote):$AppDir/"
    if ($LASTEXITCODE -ne 0) {
        throw "传输 config 失败"
    }

    Write-Host "==> 传输 $($modules.Count) 个镜像 tar" -ForegroundColor Cyan
    foreach ($module in $modules) {
        $localTar = "service-$module\target\jib-image.tar"
        $remoteTar = "$AppDir/images/service-$module.tar"
        Write-Host "    $localTar -> ${Remote}:$remoteTar"
        & scp $localTar "$($Remote):$remoteTar"
        if ($LASTEXITCODE -ne 0) {
            throw "传输 $localTar 失败"
        }
    }

    # Start 包含加载动作，避免用户只想启动却拿到未加载的旧镜像。
    if ($Load -or $Start) {
        Write-Host "==> 远程加载镜像" -ForegroundColor Cyan
        foreach ($module in $modules) {
            & ssh $Remote "docker load -i '$AppDir/images/service-$module.tar'"
            if ($LASTEXITCODE -ne 0) {
                throw "远程加载 service-$module 镜像失败"
            }
        }
    }

    if ($Start) {
        Write-Host "==> 启动远程 Compose 应用" -ForegroundColor Cyan
        & ssh $Remote "cd '$AppDir' && docker compose up -d && docker compose ps"
        if ($LASTEXITCODE -ne 0) {
            throw "远程启动 docker compose 失败"
        }
    }

    Write-Host "==> 完成" -ForegroundColor Green
    if (-not $Load -and -not $Start) {
        Write-Host "镜像尚未加载。后续在 Ubuntu 执行：" -ForegroundColor Yellow
        Write-Host "  cd $AppDir"
        Write-Host "  for service in $($modules -join ' '); do docker load -i images/service-\$service.tar; done"
        Write-Host "  cp .env.example .env && chmod 600 .env"
        Write-Host "  docker compose up -d"
    }
    if ($Tag -ne "1.0.0") {
        Write-Host "提醒：当前构建 tag 是 $Tag，请确保远程 .env 中 APP_TAG=$Tag。" -ForegroundColor Yellow
    }
}
finally {
    Pop-Location
}
