#!/usr/bin/env bash

# Figer immédiatement le chemin absolu du dossier où se trouve CE script (ops/local/scripts)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"

# Pointer de manière absolue vers le dossier docker FRÈRE (ops/local/docker)
DOCKER_DIR="$PROJECT_ROOT/ops/local/docker"
COMPOSE_FILE="$DOCKER_DIR/docker-compose.yml"

# Fichier PID dans le dossier var/ de la racine
PID_FILE="$PROJECT_ROOT/var/.orasaka.pid"

set -euo pipefail

# Load environment variables from root .env if it exists
ENV_FILE="$PROJECT_ROOT/.env"
if [ -f "$ENV_FILE" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        if [[ ! "$line" =~ ^[[:space:]]*# ]] && [[ -n "$line" ]]; then
            # Clean carriage returns and export
            clean_line=$(echo "$line" | tr -d '\r')
            key=$(echo "$clean_line" | cut -d= -f1)
            val=$(echo "$clean_line" | cut -d= -f2-)
            if [[ "$val" =~ ^var/ ]]; then
                val="$PROJECT_ROOT/$val"
            fi
            export "$key=$val"
        fi
    done < "$ENV_FILE"
fi

# Enforce strict memory hygiene and performance parameters for Ollama
export OLLAMA_NUM_PARALLEL=${OLLAMA_NUM_PARALLEL:-1}
export OLLAMA_KEEP_ALIVE=${OLLAMA_KEEP_ALIVE:-"24h"}

# --- ANSI Color Palette ---
GREEN='\033[1;32m'
RED='\033[1;31m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
CYAN='\033[1;36m'
WHITE='\033[1;37m'
RESET='\033[0m'
BOLD='\033[1m'

echo -e "${BLUE}${BOLD}=== ORASAKA LOCAL INFRASTRUCTURE BOOTSTRAP --- GENUINE FIRST ===${RESET}"
echo -e "${CYAN}Starting local environment setup...${RESET}"
echo ""

# Find Docker Compose command
DOCKER_COMPOSE_CMD=""
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
fi

if [ -z "$DOCKER_COMPOSE_CMD" ]; then
    echo -e "${RED}[ ERROR ]${RESET} ${WHITE}docker compose command not found on PATH.${RESET}"
    exit 1
fi

# ==============================================================================
# STEP 1: Dependencies Check
# ==============================================================================
echo -e "${CYAN}STEP 1: Verifying host-level development tools...${RESET}"
MISSING_TOOLS=0
for tool in terraform aws python3 modal; do
    if ! command -v "$tool" &> /dev/null; then
        echo -e "${YELLOW}[ WARN ]${RESET} Tool '$tool' is missing."
        MISSING_TOOLS=1
    fi
done

if [ $MISSING_TOOLS -eq 1 ]; then
    echo -e "${CYAN}Required tooling missing. Triggering install-tooling.sh...${RESET}"
    if [ -f "$PROJECT_ROOT/ops/local/scripts/install-tooling.sh" ]; then
        bash "$PROJECT_ROOT/ops/local/scripts/install-tooling.sh"
    else
        echo -e "${RED}[ ERROR ]${RESET} install-tooling.sh script not found."
        exit 1
    fi
fi

# ==============================================================================
# STEP 2: Docker Middleware (Postgres, Redis, RabbitMQ, MCP debug)
# ==============================================================================
echo -e "${CYAN}STEP 2: Launching Docker middleware...${RESET}"
# Spin up only the 4 middleware services
eval "$DOCKER_COMPOSE_CMD -p orasaka -f \"$COMPOSE_FILE\" up -d postgres redis rabbitmq mcp-debug-server >/dev/null"

# ==============================================================================
# STEP 3: Native Host-Level AI Engine Verification & Launch
# ==============================================================================
echo -e "${CYAN}STEP 3: Initiating Host-Level AI Engine Verification...${RESET}"
if [ -f "$SCRIPT_DIR/verify-host-services.sh" ]; then
    source "$SCRIPT_DIR/verify-host-services.sh"
else
    echo -e "${RED}[ ERROR ]${RESET} verify-host-services.sh script not found."
    exit 1
fi


# ==============================================================================
# STEP 3b: Launch Automation Worker
# ==============================================================================
echo -e "${CYAN}STEP 3b: Starting Host-Level Automation Worker...${RESET}"
if ! curl -s http://127.0.0.1:8082/actuator/health | grep -q "status" >/dev/null 2>&1; then
    mkdir -p "$PROJECT_ROOT/var/logs"
    DATE_STAMP=$(date +%Y%m%d)
    echo -e "${YELLOW}[ WARN ]${RESET} Automation Worker is not active on host. Starting...${RESET}"
    cd "$PROJECT_ROOT" && PORT=8082 mvn spring-boot:run -pl orasaka-workers/automation 2>&1 | /usr/sbin/rotatelogs -n 5 "$PROJECT_ROOT/var/logs/${DATE_STAMP}_automation-worker.log" 1M &
    AUTO_PID=$!
    echo "automation-worker=$AUTO_PID" >> "$PID_FILE"
    echo -e "${GREEN}[ OK ]${RESET} Automation Worker (PID $AUTO_PID) background task triggered."
else
    echo -e "${GREEN}[ OK ]${RESET} Automation Worker is already active on host."
fi

# ==============================================================================
# STEP 4: Absolute Health Gates
# ==============================================================================
echo -e "${CYAN}STEP 4: Entering absolute health validation gates...${RESET}"

retries=30
all_healthy=false

while [ $retries -gt 0 ]; do
    echo -e "${BLUE}Verifying status of monorepo service mesh...${RESET}"
    
    # 1. Postgres
    pg_ok=false
    if docker exec orasaka-postgres pg_isready -U orasaka_admin -d orasaka_db >/dev/null 2>&1; then
        pg_ok=true
    fi
    
    # 2. Redis
    redis_ok=false
    if docker exec orasaka-redis redis-cli ping >/dev/null 2>&1; then
        redis_ok=true
    fi
    
    # 3. RabbitMQ
    rabbitmq_ok=false
    if docker exec orasaka-rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1; then
        rabbitmq_ok=true
    fi
    
    # 4. MCP Debug Server
    mcp_ok=false
    if [ "$(docker inspect -f '{{.State.Running}}' orasaka-mcp-debug 2>/dev/null)" = "true" ]; then
        mcp_ok=true
    fi
    
    # 5. Ollama (11434)
    ollama_ok=false
    if curl -s http://127.0.0.1:11434/ >/dev/null 2>&1; then
        ollama_ok=true
    fi
    
    # 6. Image AI (8085)
    image_ok=false
    if curl -s http://127.0.0.1:8085/ >/dev/null 2>&1; then
        image_ok=true
    fi
    
    # 7. Video Worker (8188)
    video_ok=false
    if curl -s http://127.0.0.1:8188/ >/dev/null 2>&1; then
        video_ok=true
    fi

    # 8. Image Gen Worker (8086)
    image_gen_ok=false
    if curl -s http://127.0.0.1:8086/ >/dev/null 2>&1; then
        image_gen_ok=true
    fi

    # 9. Automation Worker (8082)
    auto_ok=false
    if curl -s http://127.0.0.1:8082/actuator/health | grep -q "status" >/dev/null 2>&1; then
        auto_ok=true
    fi

    # Check all
    if [ "$pg_ok" = true ] && [ "$redis_ok" = true ] && [ "$rabbitmq_ok" = true ] && [ "$mcp_ok" = true ] && [ "$ollama_ok" = true ] && [ "$image_ok" = true ] && [ "$video_ok" = true ] && [ "$image_gen_ok" = true ] && [ "$auto_ok" = true ]; then
        all_healthy=true
        break
    fi
    
    echo -e "${YELLOW}Waiting for services to become responsive... (Postgres=$pg_ok, Redis=$redis_ok, RabbitMQ=$rabbitmq_ok, MCP=$mcp_ok, Ollama=$ollama_ok, ImageAI=$image_ok, VideoWorker=$video_ok, ImageGen=$image_gen_ok, AutoWorker=$auto_ok)${RESET}"
    sleep 2
    retries=$((retries - 1))
done

if [ "$all_healthy" = false ]; then
    echo -e "\n${RED}[ ERROR ]${RESET} Monorepo services failed the absolute health gate checks."
    exit 1
fi

echo ""
echo -e "${BLUE}============================================================${RESET}"
echo -e "${GREEN}${BOLD}🏆 INFRASTRUCTURE READY 🏆${RESET}"
echo -e "${WHITE}All Docker containers, AI host endpoints, and automation worker are healthy!${RESET}"
echo -e "${BLUE}============================================================${RESET}"
echo ""