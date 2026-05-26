#!/usr/bin/env bash

# Figer immédiatement le chemin absolu du dossier où se trouve CE script (ops/local/scripts)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"

# Pointer de manière absolue vers le dossier docker FRÈRE (ops/local/docker)
DOCKER_DIR="$PROJECT_ROOT/ops/local/docker"
COMPOSE_FILE="$DOCKER_DIR/docker-compose.yml"

set -uo pipefail

# Load environment variables from root .env if it exists
ENV_FILE="$PROJECT_ROOT/.env"
if [ -f "$ENV_FILE" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        if [[ ! "$line" =~ ^[[:space:]]*# ]] && [[ -n "$line" ]]; then
            # Clean carriage returns and export
            clean_line=$(echo "$line" | tr -d '\r')
            export "$clean_line"
        fi
    done < "$ENV_FILE"
fi

# --- ANSI Color Palette ---
GREEN='\033[1;32m'
RED='\033[1;31m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
CYAN='\033[1;36m'
WHITE='\033[1;37m'
RESET='\033[0m'
BOLD='\033[1m'

echo -e "${BLUE}${BOLD}=== ORASAKA LOCAL INFRASTRUCTURE TEARDOWN ===${RESET}"
echo -e "${CYAN}Stopping local environment...${RESET}"
echo ""

PID_FILE="$PROJECT_ROOT/var/.orasaka.pid"

terminate_process() {
  local pid=$1
  local name=$2

  if [ -n "$pid" ] && ps -p "$pid" >/dev/null 2>&1; then
    echo -e "${CYAN}Terminating $name (PID $pid) gracefully...${RESET}"
    kill -15 "$pid" 2>/dev/null
    for i in {1..5}; do
      if ! ps -p "$pid" >/dev/null 2>&1; then
        echo -e "${GREEN}[ OK ]${RESET} $name terminated."
        return 0
      fi
      sleep 1
    done
    echo -e "${YELLOW}Force killing $name (PID $pid)...${RESET}"
    kill -9 "$pid" 2>/dev/null
  fi
}

# 1. State-Based Teardown
if [ -f "$PID_FILE" ]; then
  echo -e "${BLUE}Reading active PIDs from state file: $PID_FILE...${RESET}"
  while IFS= read -r line; do
    [[ "$line" =~ ^# ]] && continue
    [[ -z "$line" ]] && continue
    if [[ "$line" =~ ^([^=]+)=([0-9]+)$ ]]; then
      name="${BASH_REMATCH[1]}"
      pid="${BASH_REMATCH[2]}"
      terminate_process "$pid" "$name"
    fi
  done < "$PID_FILE"
  rm -f "$PID_FILE"
  echo -e "${GREEN}[ OK ]${RESET} State file cleaned."
fi

# 2. Total Hardware-Level Process Sweep (SIGKILL)
echo -e "${YELLOW}Sweeping active ports...${RESET}"
for port in 3000 3001 8080 8082 8085 8086 8188 11434 5432 6379 5672 15672; do
  pids=$(lsof -i :$port -t 2>/dev/null || true)
  if [ -n "$pids" ]; then
    for pid in $pids; do
      if [ -n "$pid" ] && [ "$pid" -ne "$$" ]; then
        proc_name=$(ps -p "$pid" -o comm= 2>/dev/null || true)
        if [[ "$proc_name" =~ "Docker" || "$proc_name" =~ "com.docker" ]]; then
          echo -e "${CYAN}Skipping Docker daemon process (PID $pid) on Port $port${RESET}"
        else
          echo -e "${RED}Force killing process on Port $port (PID $pid: $proc_name)...${RESET}"
          kill -9 "$pid" 2>/dev/null || true
        fi
      fi
    done
  fi
done

# 2b. Actively purge mock database entries from postgres container if running
if docker ps | grep -q "orasaka-postgres"; then
  echo -e "${CYAN}Purging active database entries from orasaka-postgres container...${RESET}"
  docker exec -i orasaka-postgres psql -U orasaka_admin -d orasaka_db -c "
    TRUNCATE TABLE QRTZ_FIRED_TRIGGERS CASCADE;
    TRUNCATE TABLE QRTZ_SIMPLE_TRIGGERS CASCADE;
    TRUNCATE TABLE QRTZ_CRON_TRIGGERS CASCADE;
    TRUNCATE TABLE QRTZ_TRIGGERS CASCADE;
    TRUNCATE TABLE QRTZ_JOB_DETAILS CASCADE;
    TRUNCATE TABLE QRTZ_SCHEDULER_STATE CASCADE;
    TRUNCATE TABLE QRTZ_LOCKS CASCADE;
    TRUNCATE TABLE automation_job_execution_log CASCADE;
    TRUNCATE TABLE connector_credentials CASCADE;
    TRUNCATE TABLE orasaka_chat_sessions CASCADE;
    TRUNCATE TABLE orasaka_jobs CASCADE;
    TRUNCATE TABLE user_mcp_servers CASCADE;
    TRUNCATE TABLE platform_mcp_servers CASCADE;
    TRUNCATE TABLE platform_tool_configs CASCADE;
    TRUNCATE TABLE user_credentials CASCADE;
    TRUNCATE TABLE orasaka_users CASCADE;
    TRUNCATE TABLE orasaka_user_profiles CASCADE;
    TRUNCATE TABLE orasaka_verification_tokens CASCADE;
    TRUNCATE TABLE orasaka_user_interceptions CASCADE;
    TRUNCATE TABLE orasaka_tools_cache CASCADE;
    TRUNCATE TABLE orasaka_tools_rag_source CASCADE;
    TRUNCATE TABLE orasaka_password_resets CASCADE;
    TRUNCATE TABLE orasaka_ai_mcp_servers CASCADE;
    TRUNCATE TABLE orasaka_ai_rag_stores CASCADE;
  " >/dev/null 2>&1 || true
fi

echo -e "${YELLOW}Sweeping target processes by pattern...${RESET}"
patterns=("ollama" "orasaka-video-worker" "orasaka-automation-worker" "orasaka-worker-automation" "orasaka-workers/automation" "sd-server" "stable-diffusion" "next-dev" "next-server" "orasaka-gateway")
for pattern in "${patterns[@]}"; do
  pids=$(pgrep -f "$pattern" || true)
  if [ -n "$pids" ]; then
    for pid in $pids; do
      if [ -n "$pid" ] && [ "$pid" -ne "$$" ] && ps -p "$pid" >/dev/null 2>&1; then
        echo -e "${RED}Force killing process matching '$pattern' (PID $pid)...${RESET}"
        kill -9 "$pid" 2>/dev/null || true
      fi
    done
  fi
done

echo -e "${YELLOW}Sweeping orphan workspace processes...${RESET}"
pids=$(ps aux | grep -E "node|java|python" | grep "orasaka" | grep -v grep | awk '{print $2}' || true)
if [ -n "$pids" ]; then
  for pid in $pids; do
    if [ -n "$pid" ] && [ "$pid" -ne "$$" ] && ps -p "$pid" >/dev/null 2>&1; then
      proc_args=$(ps -p "$pid" -o args= 2>/dev/null || true)
      if [[ "$proc_args" =~ "e2e_comprehensive.py" ]]; then
        echo -e "${CYAN}Skipping E2E runner process (PID $pid)${RESET}"
      else
        echo -e "${RED}Force killing workspace process (PID $pid: $proc_args)...${RESET}"
        kill -9 "$pid" 2>/dev/null || true
      fi
    fi
  done
fi

# 3. Teardown Docker Compose services (NUKING VOLUMES!)
DOCKER_COMPOSE_CMD=""
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
fi

if [ -n "$DOCKER_COMPOSE_CMD" ]; then
    echo -e "${YELLOW}${BOLD}[ WARN ] Stopping Docker containers gracefully...${RESET}"
    eval "$DOCKER_COMPOSE_CMD -p orasaka -f \"$COMPOSE_FILE\" stop >/dev/null 2>&1 || true"
    sleep 1
    echo -e "${YELLOW}${BOLD}[ WARN ] Purging Docker infrastructure AND data volumes...${RESET}"
    eval "$DOCKER_COMPOSE_CMD -p orasaka -f \"$COMPOSE_FILE\" down --timeout 5 -v >/dev/null 2>&1 || true"
    echo -e "${GREEN}[ OK ]${RESET} Docker services and data volumes permanently removed."
fi

# Clean local folders
echo -e "${CYAN}Purging local upload, temp directories, and PIDs...${RESET}"
rm -rf "$PROJECT_ROOT/var/orasaka-uploads/"*
rm -rf "$PROJECT_ROOT/var/temp/"*
rm -f "$PROJECT_ROOT/var/.orasaka*.pid"

# 4. Flush Apple Silicon M1 Unified Memory Pages
echo -e "${BLUE}Flushing macOS unified memory pages...${RESET}"
sudo -n purge 2>/dev/null || purge 2>/dev/null || true

echo ""
echo -e "${GREEN}${BOLD}🛑 TEARDOWN COMPLETE 🛑${RESET}"
echo -e "${WHITE}All database infrastructure, active volumes, background tasks, and memory pages purged.${RESET}"
echo ""