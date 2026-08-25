#!/usr/bin/env bash

# Docker Compose 应用维护脚本，适用于 Ubuntu/macOS 部署机。
# 前提：docker-compose.yml、.env、config/ 和镜像 tar 已经上传到部署目录。
# 默认在当前目录寻找 docker-compose.yml；也可以用 --app-dir 指定部署目录。

set -u

APP_DIR=${ZJC_APP_DIR:-$PWD}
ASSUME_YES=0
INTERACTIVE=1
COMPOSE_CMD=()
ALL_SERVICES=()
SELECTED_SERVICES=()
SELECTED_IS_ALL=0

if [ -t 1 ]; then
  COLOR_BLUE=$'\033[1;34m'
  COLOR_GREEN=$'\033[1;32m'
  COLOR_YELLOW=$'\033[1;33m'
  COLOR_RED=$'\033[1;31m'
  COLOR_RESET=$'\033[0m'
else
  COLOR_BLUE=""
  COLOR_GREEN=""
  COLOR_YELLOW=""
  COLOR_RED=""
  COLOR_RESET=""
fi

info() {
  printf '%s==> %s%s\n' "$COLOR_BLUE" "$*" "$COLOR_RESET"
}

success() {
  printf '%s==> %s%s\n' "$COLOR_GREEN" "$*" "$COLOR_RESET"
}

warn() {
  printf '%s警告：%s%s\n' "$COLOR_YELLOW" "$*" "$COLOR_RESET" >&2
}

error() {
  printf '%s错误：%s%s\n' "$COLOR_RED" "$*" "$COLOR_RESET" >&2
}

usage() {
  cat <<'EOF'
Docker Compose 应用维护脚本

用法：
  ./manage.sh                         # 进入交互菜单
  ./manage.sh [选项] <命令> [参数]

常用选项：
  --app-dir <dir>                     指定部署目录，默认当前目录
  -y, --yes                           跳过危险操作确认，仅建议自动化使用
  -h, --help                          显示帮助

命令：
  status                              查看容器状态
  services                            查看 Compose 服务列表
  start [all|service...]              启动，默认不删除、不重建已有容器
  stop [all|service...]               停止，不删除容器
  restart [all|service...]            重启，不删除容器
  config-restart [all|service...]     外置 YAML 修改后重启应用
  apply [all|service...]              按 Compose/.env 新定义应用，可重建容器
  rm <service...>                     停止并删除指定容器
  down [--remove-orphans]             停止并删除全部容器和网络
  logs [all|service...]               跟踪日志
  recent [all|service...] [lines]     查看最近日志，默认 200 行
  load                                加载 images/*.tar 镜像
  scale <service> <count>             扩缩容指定服务
  images                              查看服务使用的镜像
  validate                            校验 Compose 配置
  info                                查看配置和日志占用
  env                                 编辑 .env

示例：
  ./manage.sh
  ./manage.sh start all
  ./manage.sh config-restart service-provider
  ./manage.sh recent service-gateway 300
  ./manage.sh --app-dir /home/zjc/app status
EOF
}

resolve_app_dir() {
  if [ ! -d "$APP_DIR" ]; then
    error "部署目录不存在：$APP_DIR"
    return 1
  fi

  APP_DIR=$(cd "$APP_DIR" && pwd) || return 1

  if [ ! -f "$APP_DIR/docker-compose.yml" ] && [ ! -f "$APP_DIR/compose.yaml" ]; then
    error "部署目录中找不到 docker-compose.yml：$APP_DIR"
    error "请 cd 到部署目录执行，或使用 --app-dir 指定目录。"
    return 1
  fi

  cd "$APP_DIR" || return 1
}

detect_compose_command() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
  else
    error "未找到 Docker Compose。请安装 Docker Engine 和 Compose 插件。"
    return 1
  fi
}

compose() {
  "${COMPOSE_CMD[@]}" "$@"
}

load_services() {
  local service_text service

  if ! service_text=$(compose config --services 2>/dev/null | sort); then
    error "无法读取 Compose 服务列表，请检查 docker-compose.yml 和 .env。"
    return 1
  fi

  ALL_SERVICES=()
  while IFS= read -r service; do
    [ -n "$service" ] && ALL_SERVICES+=("$service")
  done <<EOF
$service_text
EOF

  if [ "${#ALL_SERVICES[@]}" -eq 0 ]; then
    error "Compose 文件中没有服务定义。"
    return 1
  fi
}

print_services() {
  local index service
  index=1
  for service in "${ALL_SERVICES[@]}"; do
    printf '  %d. %s\n' "$index" "$service"
    index=$((index + 1))
  done
}

reset_selection() {
  SELECTED_SERVICES=()
  SELECTED_IS_ALL=0
}

service_exists() {
  local service
  for service in "${ALL_SERVICES[@]}"; do
    [ "$service" = "$1" ] && return 0
  done
  return 1
}

append_selected_service() {
  local service existing
  for existing in "${SELECTED_SERVICES[@]}"; do
    [ "$existing" = "$1" ] && return 0
  done
  SELECTED_SERVICES+=("$1")
}

normalize_services() {
  local input item service index
  reset_selection

  for input in "$@"; do
    item=${input//,/ }
    for service in $item; do
      case "$service" in
        all|ALL)
          if [ "${#SELECTED_SERVICES[@]}" -gt 0 ] || [ "$SELECTED_IS_ALL" -eq 1 ]; then
            error "all 不能和具体服务混用。"
            return 1
          fi
          SELECTED_IS_ALL=1
          ;;
        *[!0-9]*)
          if ! service_exists "$service"; then
            error "未知服务：$service"
            return 1
          fi
          append_selected_service "$service"
          ;;
        *)
          if [ "$service" -lt 1 ] || [ "$service" -gt "${#ALL_SERVICES[@]}" ]; then
            error "服务编号超出范围：$service"
            return 1
          fi
          index=$((service - 1))
          append_selected_service "${ALL_SERVICES[$index]}"
          ;;
      esac
    done
  done

  if [ "$SELECTED_IS_ALL" -eq 0 ] && [ "${#SELECTED_SERVICES[@]}" -eq 0 ]; then
    error "没有选择任何服务。"
    return 1
  fi
  if [ "$SELECTED_IS_ALL" -eq 1 ] && [ "${#SELECTED_SERVICES[@]}" -gt 0 ]; then
    error "all 不能和具体服务混用。"
    return 1
  fi
  return 0
}

select_services() {
  local reply
  printf '%s\n' "可用服务："
  print_services
  printf '%s' "请输入服务编号或名称，多个用空格/逗号分隔，all 表示全部："
  read -r reply

  if [ -z "$reply" ]; then
    warn "未输入服务，本次操作已取消。"
    return 1
  fi

  normalize_services "$reply"
}

selected_description() {
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    printf '全部服务'
  else
    printf '%s' "${SELECTED_SERVICES[*]}"
  fi
}

pause_when_interactive() {
  local reply
  [ "$INTERACTIVE" -eq 0 ] && return 0
  printf '%s' "按回车返回菜单..."
  read -r reply
}

confirm() {
  local reply
  if [ "$ASSUME_YES" -eq 1 ]; then
    return 0
  fi

  printf '%s' "$1 [y/N]: "
  read -r reply
  case "$reply" in
    y|Y|yes|YES) return 0 ;;
    *) return 1 ;;
  esac
}

env_exists() {
  [ -f ".env" ]
}

require_env() {
  if env_exists; then
    return 0
  fi

  error "部署目录缺少 .env：$APP_DIR/.env"
  error "首次使用请执行：cp .env.example .env && chmod 600 .env && vi .env"
  return 1
}

show_status() {
  info "容器状态"
  compose ps || return 1
}

show_services() {
  info "Compose 服务"
  print_services
}

show_images() {
  info "服务镜像"
  compose images || return 1
}

validate_config() {
  info "校验 Compose 配置"
  if compose config --quiet; then
    success "Compose 配置校验通过。"
  else
    error "Compose 配置校验失败。"
    return 1
  fi
}

start_services() {
  require_env || return 1
  info "启动：$(selected_description)"
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    compose up -d --no-recreate || return 1
  else
    compose up -d --no-recreate "${SELECTED_SERVICES[@]}" || return 1
  fi
  compose ps
  success "启动命令已执行。"
}

stop_services() {
  require_env || return 1
  info "停止（保留容器）：$(selected_description)"
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    compose stop || return 1
  else
    compose stop "${SELECTED_SERVICES[@]}" || return 1
  fi
  success "停止命令已执行。"
}

restart_services() {
  require_env || return 1
  info "重启（保留容器）：$(selected_description)"
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    compose restart || return 1
  else
    compose restart "${SELECTED_SERVICES[@]}" || return 1
  fi
  success "重启命令已执行。"
}

apply_services() {
  require_env || return 1
  info "应用 Compose 定义：$(selected_description)"
  info "该操作会按需创建或重建容器，用于镜像 tag、.env、Compose 文件变更。"
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    compose up -d || return 1
  else
    compose up -d "${SELECTED_SERVICES[@]}" || return 1
  fi
  compose ps
  success "Compose 定义已应用。"
}

remove_selected_services() {
  require_env || return 1

  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    error "请选择具体服务；删除全部请使用 down。"
    return 1
  fi

  info "将停止并删除容器：${SELECTED_SERVICES[*]}"
  if ! confirm "确认删除这些容器？"; then
    warn "已取消。"
    return 0
  fi

  compose rm -sf "${SELECTED_SERVICES[@]}" || return 1
  success "指定容器已删除。镜像、配置和日志会保留。"
}

remove_all_services() {
  require_env || return 1
  local extra_args=()

  if [ "${1:-}" = "--remove-orphans" ]; then
    extra_args+=(--remove-orphans)
  elif [ -n "${1:-}" ]; then
    error "down 不支持参数：$1"
    return 1
  fi

  info "将停止并删除全部容器和 Compose 网络"
  warn "镜像、.env、外置配置、应用日志会保留；gateway 宿主机 80 会释放。"
  if ! confirm "确认删除全部容器？"; then
    warn "已取消。"
    return 0
  fi

  compose down "${extra_args[@]+"${extra_args[@]}"}" || return 1
  success "全部容器和网络已删除。"
}

follow_logs() {
  require_env || return 1
  info "跟踪日志：$(selected_description)"
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    compose logs -f --tail=200
  else
    compose logs -f --tail=200 "${SELECTED_SERVICES[@]}"
  fi
}

show_recent_logs() {
  require_env || return 1
  local lines="${1:-200}"

  case "$lines" in
    *[!0-9]*|'')
      error "日志行数必须是正整数：$lines"
      return 1
      ;;
    *)
      if [ "$lines" -lt 1 ]; then
        error "日志行数必须大于 0。"
        return 1
      fi
      ;;
  esac

  info "查看最近 ${lines} 行日志：$(selected_description)"
  if [ "$SELECTED_IS_ALL" -eq 1 ]; then
    compose logs --tail="$lines"
  else
    compose logs --tail="$lines" "${SELECTED_SERVICES[@]}"
  fi
}

load_image_tars() {
  local tar found=0
  shopt -s nullglob

  info "加载镜像 tar"
  for tar in images/*.tar; do
    found=1
    printf '加载 %s\n' "$tar"
    docker load -i "$tar" || return 1
  done

  if [ "$found" -eq 0 ]; then
    error "部署目录 images/ 下没有镜像 tar。"
    return 1
  fi

  success "镜像加载完成。若镜像 tag 更新，请执行 apply 让容器重建。"
}

scale_service() {
  require_env || return 1
  local service count

  if [ "$SELECTED_IS_ALL" -eq 1 ] || [ "${#SELECTED_SERVICES[@]}" -ne 1 ]; then
    error "扩缩容一次只能选择一个服务。"
    return 1
  fi

  service="${SELECTED_SERVICES[0]}"
  if ! service_exists "$service"; then
    error "未知服务：$service"
    return 1
  fi

  if [ "$#" -eq 1 ]; then
    count="$1"
  else
    printf '%s' "请输入 ${service} 的实例数："
    read -r count
  fi

  case "$count" in
    *[!0-9]*|'')
      error "实例数必须是正整数：$count"
      return 1
      ;;
    *)
      if [ "$count" -lt 1 ]; then
        error "实例数必须大于 0。"
        return 1
      fi
      ;;
  esac

  if [ "$service" = "service-gateway" ] && [ "$count" -gt 1 ]; then
    error "gateway 固定映射宿主机 80，多实例会端口冲突。请先引入外部负载均衡后再扩容。"
    return 1
  fi

  info "调整 ${service} 实例数为 ${count}"
  compose up -d --scale "${service}=${count}" || return 1
  compose ps
  success "扩缩容命令已执行。"
}

show_deploy_info() {
  info "部署目录：$APP_DIR"
  info "外置配置文件"
  find config -type f \( -name '*.yaml' -o -name '*.yml' -o -name '*.template' -o -name '.env*' \) -print 2>/dev/null | sort

  info "日志目录占用"
  du -sh logs 2>/dev/null || warn "logs 目录不存在。"

  info "镜像 tar"
  find images -maxdepth 1 -type f -name '*.tar' -print 2>/dev/null | sort
}

edit_env_file() {
  local editor

  if [ "$INTERACTIVE" -eq 0 ]; then
    error "env 命令需要在交互终端中执行。"
    return 1
  fi

  if ! env_exists; then
    if [ ! -f ".env.example" ]; then
      error "找不到 .env 和 .env.example。"
      return 1
    fi
    if confirm ".env 不存在，是否从 .env.example 创建？"; then
      cp .env.example .env || return 1
      chmod 600 .env || return 1
    else
      warn "已取消。"
      return 0
    fi
  fi

  chmod 600 .env
  editor=${EDITOR:-vi}
  info "编辑 .env（不会把内容打印到终端）"
  "$editor" .env
  warn "如果修改了 APP_TAG、SPRING_PROFILE 或 Jasypt 主密钥，需要执行 apply 让容器重建。"
}

menu_config_restart() {
  if ! select_services; then
    return 0
  fi
  warn "外置 YAML 只需要 restart；修改 .env 或 docker-compose.yml 时请使用 apply。"
  restart_services
}

menu_apply() {
  if ! select_services; then
    return 0
  fi
  apply_services
}

menu_action_with_selection() {
  local action="$1"
  if ! select_services; then
    return 0
  fi
  case "$action" in
    start) start_services ;;
    stop) stop_services ;;
    restart) restart_services ;;
    *) error "未知菜单操作：$action" ;;
  esac
}

menu_remove_selected() {
  if ! select_services; then
    return 0
  fi
  remove_selected_services
}

menu_recent_logs() {
  local lines
  if ! select_services; then
    return 0
  fi
  printf '%s' "请输入日志行数，默认 200："
  read -r lines
  lines=${lines:-200}
  show_recent_logs "$lines"
}

menu() {
  local choice

  while true; do
    printf '\n%sDocker Compose 应用维护%s\n' "$COLOR_BLUE" "$COLOR_RESET"
    printf '部署目录：%s\n' "$APP_DIR"
    printf 'Compose：%s\n' "${COMPOSE_CMD[*]}"
    if ! env_exists; then
      warn "当前目录还没有 .env，启动前请先创建并填写。"
    fi

    cat <<'EOF'

  1. 查看容器状态
  2. 启动全部服务
  3. 停止全部服务（保留容器）
  4. 重启全部服务（保留容器）
  5. 启动指定服务
  6. 停止指定服务（保留容器）
  7. 重启指定服务（保留容器）
  8. 外置 YAML 修改后重启
  9. 应用 Compose/.env 变更
 10. 停止并删除指定容器
 11. 停止并删除全部容器
 12. 跟踪全部日志
 13. 跟踪指定服务日志
 14. 查看最近日志
 15. 加载已上传的镜像 tar
 16. 扩缩容指定服务
 17. 查看服务镜像
 18. 查看服务和配置清单
 19. 校验 Compose 配置
 20. 编辑 .env
  0. 退出
EOF

    printf '%s' "请选择操作 [0-20]："
    read -r choice
    case "$choice" in
      0) success "已退出。" ; exit 0 ;;
      1) show_status ;;
      2) reset_selection; SELECTED_IS_ALL=1; start_services ;;
      3) reset_selection; SELECTED_IS_ALL=1; stop_services ;;
      4) reset_selection; SELECTED_IS_ALL=1; restart_services ;;
      5) menu_action_with_selection start ;;
      6) menu_action_with_selection stop ;;
      7) menu_action_with_selection restart ;;
      8) menu_config_restart ;;
      9) menu_apply ;;
      10) menu_remove_selected ;;
      11) remove_all_services ;;
      12) reset_selection; SELECTED_IS_ALL=1; follow_logs ;;
      13) if select_services; then follow_logs; fi ;;
      14) menu_recent_logs ;;
      15) load_image_tars ;;
      16) if select_services; then scale_service; fi ;;
      17) show_images ;;
      18) show_deploy_info ;;
      19) validate_config ;;
      20) edit_env_file ;;
      *) warn "无效选项：$choice" ;;
    esac
    pause_when_interactive
  done
}

main() {
  local command_name="${1:-menu}" lines last_arg service_args=() scale_service_name scale_count
  shift || true

  resolve_app_dir || return 1
  detect_compose_command || return 1
  load_services || return 1

  case "$command_name" in
    menu)
      INTERACTIVE=1
      menu
      ;;
    status)
      INTERACTIVE=0
      show_status
      ;;
    services)
      INTERACTIVE=0
      show_services
      ;;
    images)
      INTERACTIVE=0
      show_images
      ;;
    validate)
      INTERACTIVE=0
      validate_config
      ;;
    info)
      INTERACTIVE=0
      show_deploy_info
      ;;
    start|stop|restart|config-restart|apply)
      INTERACTIVE=0
      if [ "$#" -eq 0 ]; then
        set -- all
      fi
      normalize_services "$@" || return 1
      case "$command_name" in
        start) start_services ;;
        stop) stop_services ;;
        restart|config-restart) restart_services ;;
        apply) apply_services ;;
      esac
      ;;
    rm)
      INTERACTIVE=0
      if [ "$#" -eq 0 ]; then
        error "rm 需要指定至少一个服务，例如：./manage.sh rm service-provider"
        return 1
      fi
      normalize_services "$@" || return 1
      remove_selected_services
      ;;
    down)
      INTERACTIVE=0
      remove_all_services "${1:-}"
      ;;
    logs)
      INTERACTIVE=0
      if [ "$#" -eq 0 ]; then
        set -- all
      fi
      normalize_services "$@" || return 1
      follow_logs
      ;;
    recent)
      INTERACTIVE=0
      lines=200
      if [ "$#" -gt 0 ]; then
        last_arg="${!#}"
        case "$last_arg" in
          *[!0-9]*|'') service_args=("$@") ;;
          *)
            lines="$last_arg"
            if [ "$#" -gt 1 ]; then
              service_args=("${@:1:$(( $# - 1 ))}")
            fi
            ;;
        esac
      fi
      if [ "${#service_args[@]}" -eq 0 ]; then
        service_args=(all)
      fi
      normalize_services "${service_args[@]}" || return 1
      show_recent_logs "$lines"
      ;;
    load)
      INTERACTIVE=0
      load_image_tars
      ;;
    scale)
      INTERACTIVE=0
      if [ "$#" -ne 2 ]; then
        error "用法：./manage.sh scale <service> <count>"
        return 1
      fi
      scale_service_name="$1"
      scale_count="$2"
      case "$scale_count" in
        *[!0-9]*|'') error "实例数必须是正整数：$scale_count"; return 1 ;;
      esac
      [ "$scale_count" -ge 1 ] || {
        error "实例数必须大于 0。"
        return 1
      }
      if [ "$scale_service_name" = "service-gateway" ] && [ "$scale_count" -gt 1 ]; then
        error "gateway 固定映射宿主机 80，不能直接扩成多实例。"
        return 1
      fi
      reset_selection
      SELECTED_SERVICES=("$scale_service_name")
      scale_service "$scale_count"
      ;;
    env)
      INTERACTIVE=1
      edit_env_file
      ;;
    *)
      error "未知命令：$command_name"
      usage
      return 64
      ;;
  esac
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --app-dir)
      if [ "$#" -lt 2 ]; then
        error "--app-dir 需要一个目录参数。"
        exit 64
      fi
      APP_DIR="$2"
      shift 2
      ;;
    --app-dir=*)
      APP_DIR="${1#--app-dir=}"
      shift
      ;;
    -y|--yes)
      ASSUME_YES=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      break
      ;;
    *)
      break
      ;;
  esac
done

main "$@"
