#!/bin/bash
# ============================================================
#  Spring Cloud Alibaba - Service Manager (macOS / Linux)
#  Auto-discovers services under build/, excludes service-common.
#  Interactive menu + CLI: start/stop/restart/status [all|<name>]
# ============================================================

# Auto-detect: use build/ subdirectory if it exists, otherwise use script's own directory
if [ -d "$(cd "$(dirname "$0")" && pwd)/build" ]; then
    BUILD_DIR="$(cd "$(dirname "$0")/build" && pwd)"
else
    BUILD_DIR="$(cd "$(dirname "$0")" && pwd)"
fi
JVM_OPTS="-Xms256m -Xmx512m"
LOADER="lib"
EXCLUDE="service-common"
GREEN='\033[0;32m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

# ---------- scan services ----------

get_services() {
    local svcs=()
    for d in "$BUILD_DIR"/*/; do
        [ -d "$d" ] || continue
        local name=$(basename "$d")
        [ "$name" = "$EXCLUDE" ] && continue
        local jar=$(ls "$d"*.jar 2>/dev/null | grep -v sources | head -1)
        [ -z "$jar" ] && continue
        svcs+=("$name")
    done
    printf '%s\n' "${svcs[@]}" | sort
}

get_status() {
    local name="$1"
    local pid_file="$BUILD_DIR/$name/$name.pid"
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file" 2>/dev/null)
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            echo "RUNNING:$pid"
            return
        fi
    fi
    echo "STOPPED:"
}

# ---------- actions ----------

do_start() {
    local name="$1"
    local dir="$BUILD_DIR/$name"
    local jar=$(ls "$dir"/*.jar 2>/dev/null | grep -v sources | head -1)
    local st=$(get_status "$name")
    if [[ "$st" == RUNNING:* ]]; then
        echo -e "  ${YELLOW}$name is already running (PID: ${st#RUNNING:})${NC}"
        return
    fi
    echo -ne "  Starting $name ..."
    pushd "$dir" > /dev/null
    nohup java $JVM_OPTS -Dloader.path=$LOADER -jar "$jar" \
        > "$name.out" 2> "$name.err" &
    local pid=$!
    echo "$pid" > "$name.pid"
    popd > /dev/null
    echo -e " ${GREEN}OK (PID: $pid)${NC}"
}

do_stop() {
    local name="$1"
    local dir="$BUILD_DIR/$name"
    local st=$(get_status "$name")
    if [[ "$st" != RUNNING:* ]]; then
        echo -e "  ${GRAY}$name is not running${NC}"
        return
    fi
    local pid=${st#RUNNING:}
    echo -ne "  Stopping $name (PID: $pid) ..."
    kill "$pid" 2>/dev/null
    sleep 1
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
    rm -f "$dir/$name.pid"
    echo -e " ${CYAN}Done${NC}"
}

do_restart() {
    do_stop "$1"
    sleep 1
    do_start "$1"
}

# ---------- interactive menu ----------

show_dashboard() {
    clear
    echo ""
    echo -e "  ${GRAY}================================================${NC}"
    echo -e "  ${CYAN}   Spring Cloud Alibaba - Service Manager${NC}"
    echo -e "  ${GRAY}================================================${NC}"
    echo ""
    local services=("$@")
    local i=1
    for name in "${services[@]}"; do
        local st=$(get_status "$name")
        if [[ "$st" == RUNNING:* ]]; then
            printf "  [%d] %-22s " "$i" "$name"
            echo -e "${GREEN}RUNNING  (PID: ${st#RUNNING:})${NC}"
        else
            printf "  [%d] %-22s " "$i" "$name"
            echo -e "${GRAY}STOPPED${NC}"
        fi
        ((i++))
    done
    echo ""
    echo -e "  ${GRAY}[a] Start all    [s] Stop all    [x] Restart all${NC}"
    echo -e "  ${GRAY}[r] Refresh      [q] Quit${NC}"
    echo ""
}

service_submenu() {
    local name="$1"
    local st=$(get_status "$name")
    local state="${st%%:*}"
    local pid="${st#*:}"
    echo ""
    if [[ "$state" == "RUNNING" ]]; then
        echo -e "  ${CYAN}$name - RUNNING (PID: $pid)${NC}"
        echo ""
        echo "  [1] Stop      [2] Restart      [b] Back"
    else
        echo -e "  ${CYAN}$name - STOPPED${NC}"
        echo ""
        echo "  [1] Start                        [b] Back"
    fi
    echo ""
    read -p "  Action: " sub
    if [[ "$state" == "RUNNING" ]]; then
        case "$sub" in
            1) do_stop "$name"; sleep 1 ;;
            2) do_restart "$name"; sleep 1 ;;
        esac
    else
        [ "$sub" = "1" ] && { do_start "$name"; sleep 2; }
    fi
}

run_menu() {
    while true; do
        mapfile -t services < <(get_services)
        if [ ${#services[@]} -eq 0 ]; then
            echo -e "  ${RED}No services found under $BUILD_DIR${NC}"
            return
        fi
        show_dashboard "${services[@]}"
        read -p "  Enter choice: " choice
        case "$choice" in
            q|Q) break ;;
            r|R) continue ;;
            a|A) echo ""; for s in "${services[@]}"; do do_start "$s"; done; read -p $'\n  Press Enter...' ;;
            s|S) echo ""; for s in "${services[@]}"; do do_stop "$s"; done; read -p $'\n  Press Enter...' ;;
            x|X) echo ""; for s in "${services[@]}"; do do_restart "$s"; done; read -p $'\n  Press Enter...' ;;
            ''|*[!0-9]*) ;;
            *)
                if [ "$choice" -ge 1 ] 2>/dev/null && [ "$choice" -le "${#services[@]}" ]; then
                    service_submenu "${services[$((choice-1))]}"
                fi
                ;;
        esac
    done
}

# ---------- CLI mode ----------

run_cli() {
    local cmd="$1"
    local target="$2"
    mapfile -t services < <(get_services)
    if [ -n "$target" ] && [ "$target" != "all" ]; then
        local found=""
        for s in "${services[@]}"; do
            [ "$s" = "$target" ] && found="$s"
        done
        if [ -z "$found" ]; then
            echo -e "  ${RED}Unknown service: $target${NC}"
            return
        fi
        services=("$found")
    fi
    case "$cmd" in
        start)   for s in "${services[@]}"; do do_start   "$s"; done ;;
        stop)    for s in "${services[@]}"; do do_stop    "$s"; done ;;
        restart) for s in "${services[@]}"; do do_restart "$s"; done ;;
        status)
            echo ""
            for s in "${services[@]}"; do
                local st=$(get_status "$s")
                if [[ "$st" == RUNNING:* ]]; then
                    printf "  %-22s " "$s"
                    echo -e "${GREEN}RUNNING  (PID: ${st#RUNNING:})${NC}"
                else
                    printf "  %-22s " "$s"
                    echo -e "${GRAY}STOPPED${NC}"
                fi
            done
            echo ""
            ;;
        *)
            echo "  Usage: $0 <start|stop|restart|status> [all|<name>]"
            ;;
    esac
}

# ---------- entry ----------

if [ $# -gt 0 ]; then
    run_cli "$1" "$2"
else
    run_menu
fi