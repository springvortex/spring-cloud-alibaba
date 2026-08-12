#Requires -Version 5.1
<#
    Spring Cloud Alibaba - Service Manager (Windows)
    Auto-discovers services under build/, excludes service-common.
    Interactive menu + CLI: start/stop/restart/status [all|<name>]

    Jasypt 密钥传入方式（优先级从高到低）：
      1. 命令行第三个参数：  service.bat start all jasypt.encryptor.password=xxxxx
      2. 环境变量：           $env:JASYPT_ENCRYPTOR_PASSWORD = "xxxxx"
#>

# Auto-detect: use build/ subdirectory if it exists, otherwise use script's own directory
$_candidate = Join-Path $PSScriptRoot "build"
$BuildDir   = if (Test-Path $_candidate) { $_candidate } else { $PSScriptRoot }
$JVM_OPTS   = "-Xms256m -Xmx512m"
$LOADER     = "lib"
$EXCLUDE    = @("service-common")

# ---------- jasypt password resolution ----------

$JasyptProp = ""

function Resolve-Jasypt {
    # CLI 参数传入（第三个参数起，格式 key=value）
    if ($script:JasyptArgs) {
        foreach ($a in $script:JasyptArgs) {
            if ($a -match "^jasypt\.encryptor\.password=") {
                $script:JasyptProp = "-D" + $a
                return
            }
        }
    }
    # 环境变量传入
    $envPwd = $env:JASYPT_ENCRYPTOR_PASSWORD
    if ($envPwd) {
        $script:JasyptProp = "-Djasypt.encryptor.password=$envPwd"
    }
}

# ---------- scan services ----------

function Get-Services {
    $list = @()
    Get-ChildItem $BuildDir -Directory | ForEach-Object {
        if ($EXCLUDE -contains $_.Name) { return }
        $jar = Get-ChildItem $_.FullName -Filter "*.jar" |
               Where-Object { $_.Name -notlike "*-sources.jar" } |
               Select-Object -First 1
        if ($jar) {
            $list += [PSCustomObject]@{ Name = $_.Name; Dir = $_.FullName; Jar = $jar.Name }
        }
    }
    return $list | Sort-Object Name
}

function Get-Status {
    param($Svc)
    $pidFile = Join-Path $Svc.Dir "$($Svc.Name).pid"
    if (Test-Path $pidFile) {
        $procId = Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($procId -and (Get-Process -Id $procId -ErrorAction SilentlyContinue)) {
            return @{ State = "RUNNING"; PID = $procId }
        }
    }
    return @{ State = "STOPPED"; PID = $null }
}

# ---------- actions ----------

function Start-One {
    param($Svc)
    $st = Get-Status $Svc
    if ($st.State -eq "RUNNING") {
        Write-Host "  $($Svc.Name) is already running (PID: $($st.PID))" -ForegroundColor Yellow
        return
    }
    $logFile = Join-Path $Svc.Dir "$($Svc.Name).out"
    $errFile = Join-Path $Svc.Dir "$($Svc.Name).err"
    Write-Host -NoNewline "  Starting $($Svc.Name) ..."
    $argsStr = "$JVM_OPTS $JasyptProp -Dloader.path=$LOADER -jar $($Svc.Jar)"
    $proc = Start-Process -FilePath "java" `
        -ArgumentList $argsStr `
        -WorkingDirectory $Svc.Dir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError $errFile `
        -PassThru
    Set-Content -Path (Join-Path $Svc.Dir "$($Svc.Name).pid") -Value $proc.Id
    Write-Host " OK (PID: $($proc.Id))" -ForegroundColor Green
}

function Stop-One {
    param($Svc)
    $st = Get-Status $Svc
    if ($st.State -ne "RUNNING") {
        Write-Host "  $($Svc.Name) is not running" -ForegroundColor DarkGray
        return
    }
    Write-Host -NoNewline "  Stopping $($Svc.Name) (PID: $($st.PID)) ..."
    Stop-Process -Id $st.PID -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 800
    Remove-Item (Join-Path $Svc.Dir "$($Svc.Name).pid") -Force -ErrorAction SilentlyContinue
    Write-Host " Done" -ForegroundColor Cyan
}

function Restart-One {
    param($Svc)
    Stop-One $Svc
    Start-Sleep -Seconds 1
    Start-One $Svc
}

# ---------- interactive menu ----------

function Show-Dashboard {
    param($Services)
    Clear-Host
    Write-Host ""
    Write-Host "  ================================================" -ForegroundColor DarkCyan
    Write-Host "    Spring Cloud Alibaba - Service Manager"        -ForegroundColor Cyan
    Write-Host "  ================================================" -ForegroundColor DarkCyan
    Write-Host ""
    for ($i = 0; $i -lt $Services.Count; $i++) {
        $svc = $Services[$i]
        $st  = Get-Status $svc
        $tag = if ($st.State -eq "RUNNING") {
            "RUNNING  (PID: $($st.PID))"
        } else {
            "STOPPED"
        }
        $color = if ($st.State -eq "RUNNING") { "Green" } else { "DarkGray" }
        Write-Host ("  [{0}] {1,-22} " -f ($i + 1), $svc.Name) -NoNewline
        Write-Host $tag -ForegroundColor $color
    }
    Write-Host ""
    if ($JasyptProp) {
        Write-Host "  Jasypt: ENABLED" -ForegroundColor Green
    } else {
        Write-Host "  Jasypt: DISABLED (no password)" -ForegroundColor DarkGray
    }
    Write-Host ""
    Write-Host "  [a] Start all    [s] Stop all    [x] Restart all" -ForegroundColor DarkCyan
    Write-Host "  [r] Refresh      [q] Quit"                         -ForegroundColor DarkCyan
    Write-Host ""
}

function Service-SubMenu {
    param($Svc)
    $st = Get-Status $Svc
    Write-Host ""
    Write-Host "  $($Svc.Name) - $($st.State)" -ForegroundColor Cyan -NoNewline
    if ($st.PID) { Write-Host " (PID: $($st.PID))" } else { Write-Host "" }
    Write-Host ""
    if ($st.State -eq "RUNNING") {
        Write-Host "  [1] Stop      [2] Restart      [b] Back"
    } else {
        Write-Host "  [1] Start                        [b] Back"
    }
    Write-Host ""
    $sub = (Read-Host "  Action").Trim()
    if ($st.State -eq "RUNNING") {
        switch ($sub) {
            "1" { Stop-One $Svc; Start-Sleep 1 }
            "2" { Restart-One $Svc; Start-Sleep 1 }
        }
    } else {
        if ($sub -eq "1") { Start-One $Svc; Start-Sleep 2 }
    }
}

function Run-Menu {
    :main while ($true) {
        $services = Get-Services
        if ($services.Count -eq 0) {
            Write-Host "  No services found under $BuildDir" -ForegroundColor Red
            return
        }
        Show-Dashboard $services
        $choice = (Read-Host "  Enter choice").Trim().ToLower()

        switch ($choice) {
            "q" { break main }
            "r" { continue }
            "a" { Write-Host ""; $services | ForEach-Object { Start-One $_ }; Read-Host "`n  Press Enter" }
            "s" { Write-Host ""; $services | ForEach-Object { Stop-One $_ }; Read-Host "`n  Press Enter" }
            "x" { Write-Host ""; $services | ForEach-Object { Restart-One $_ }; Read-Host "`n  Press Enter" }
            default {
                $idx = 0
                if ([int]::TryParse($choice, [ref]$idx) -and $idx -ge 1 -and $idx -le $services.Count) {
                    Service-SubMenu $services[$idx - 1]
                }
            }
        }
    }
}

# ---------- CLI mode ----------

function Run-CLI {
    param([string]$Cmd, [string]$Target)
    $services = Get-Services
    if ($Target -and $Target -ne "all") {
        $services = $services | Where-Object { $_.Name -eq $Target }
        if (-not $services) {
            Write-Host "  Unknown service: $Target" -ForegroundColor Red
            return
        }
    }
    switch ($Cmd) {
        "start"   { $services | ForEach-Object { Start-One   $_ } }
        "stop"    { $services | ForEach-Object { Stop-One    $_ } }
        "restart" { $services | ForEach-Object { Restart-One $_ } }
        "status"  {
            Write-Host ""
            foreach ($svc in $services) {
                $st = Get-Status $svc
                $tag   = if ($st.State -eq "RUNNING") { "RUNNING  (PID: $($st.PID))" } else { "STOPPED" }
                $color = if ($st.State -eq "RUNNING") { "Green" } else { "DarkGray" }
                Write-Host ("  {0,-22} " -f $svc.Name) -NoNewline
                Write-Host $tag -ForegroundColor $color
            }
            Write-Host ""
        }
        default {
            Write-Host "  Usage: service <start|stop|restart|status> [all|<name>] [key=value ...]"
        }
    }
}

# ---------- entry ----------

if ($args.Count -gt 0) {
    # Collect extra args (key=value format) starting from index 2
    if ($args.Count -gt 2) {
        $script:JasyptArgs = $args[2..($args.Count - 1)]
    }
    Resolve-Jasypt
    Run-CLI -Cmd $args[0].ToLower() -Target $(if ($args.Count -gt 1) { $args[1] })
} else {
    Resolve-Jasypt
    Run-Menu
}