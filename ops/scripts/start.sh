#!/usr/bin/env bash

# Determine script directory and navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/../.."

# ==============================================================================
# ORASAKA LOCAL INFRASTRUCTURE BOOTSTRAPPER ENGINE
# ==============================================================================
# Automatically manages local process lifecycle of Ollama and sd-server workers
# ==============================================================================

set -euo pipefail

# --- ANSI Color Palette ---
RED='\033[1;31m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
MAGENTA='\033[1;35m'
CYAN='\033[1;36m'
WHITE='\033[1;37m'
BG_RED='\033[41;1;37m'
BG_BLUE='\033[44;1;37m'
RESET='\033[0m'
BOLD='\033[1m'
DIM='\033[2m'

# Handle User Interruption (Ctrl+C)
trap 'echo -e "\n${BG_RED} ABORTED ${RESET} ${RED}Bootstrap execution cancelled by user.${RESET}\n"; exit 1' INT

typewriter() {
    local text="$1"
    local color="$2"
    local delay=0.015
    echo -ne "${color}"
    for (( i=0; i<${#text}; i++ )); do
        printf "%s" "${text:$i:1}"
        sleep $delay
    done
    echo -e "${RESET}"
}

draw_gauge() {
    local label="$1"
    local fill="████████████████████"
    echo -e "${DIM}${label}${RESET} [${GREEN}${fill}${RESET}] ${YELLOW}MAX${RESET}"
}

loading_screen() {
    local pid=$1
    local delay=0.1
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while kill -0 "$pid" 2>/dev/null; do
        local temp=${spinstr#?}
        printf " [%c]  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

step_header() {
    local stage="$1"
    local check_name="$2"
    echo ""
    echo -e "${CYAN}${BOLD}=== SYSTEM BOOTSTRAP ${stage} ===${RESET}"
    echo -e "${BLUE}ORASAKA${RESET} ${YELLOW}${BOLD}::${RESET} ${WHITE}${check_name}...${RESET}"
    echo -e "${DIM}----------------------------------------${RESET}"
}

print_logo() {
    clear
    echo -e "${MAGENTA}${BOLD}"
    cat << "EOF"
  ____   ____      _      ___     _      _  __     _    
 / __ \ |  _ \    / \    / __|   / \    | |/ /    / \   
| |  | || |_) |  / _ \   \__ \  / _ \   | ' /    / _ \  
| |__| ||  _ <  / ___ \  |___/ / ___ \  | . \   / ___ \ 
 \____/ |_| \_\/_/   \_\ |___//_/   \_\ |_|\_\ /_/   \_\
EOF
    echo -e "${RESET}"
    typewriter ">>> ORASAKA BOOTSTRAPPER ENGINE <<<" "${WHITE}${BOLD}"
    echo ""
}

print_logo

# --- Verification Logic ---

check_java() {
    step_header "1" "Validating Java 21 Runtime"
    sleep 0.5 & loading_screen $!
    
    if command -v java >/dev/null 2>&1; then
        JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//;s/\..*//')
        if [ "$JAVA_VER" -lt 21 ]; then
            echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Java 21+ required. Detected version: v${JAVA_VER}${RESET}"
            exit 1
        fi
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Java 21 runtime verified. [v${JAVA_VER}]${RESET}"
    else
        echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Java runtime (java) not found on PATH.${RESET}"
        exit 1
    fi
}

check_ollama() {
    step_header "2" "Initializing Native Ollama API"
    OLLAMA_PID=""
    
    if lsof -i :11434 -t >/dev/null 2>&1; then
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Ollama service is already active on port 11434.${RESET}"
    else
        echo -e "${CYAN}Starting Ollama Core Client...${RESET}"
        ollama serve >/dev/null 2>&1 &
        OLLAMA_PID=$!
        sleep 1 & loading_screen $!
        for i in {1..10}; do
            if lsof -i :11434 -t >/dev/null 2>&1; then
                echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Ollama bound to port 11434 successfully.${RESET}"
                break
            fi
            sleep 1
        done
    fi
}

check_ollama_models() {
    step_header "3" "Validating Local Ollama Models"
    
    if ! command -v ollama >/dev/null 2>&1; then
        echo -e "${BG_RED} ERROR ${RESET} ${WHITE}ollama CLI command not found on PATH.${RESET}"
        exit 1
    fi
    
    if ! ollama list | grep -q "llama3:8b"; then
        echo -e "${YELLOW}WARNING:${RESET} ${WHITE}Model 'llama3:8b' missing. Fetching model...${RESET}"
        ollama pull llama3:8b >/dev/null 2>&1
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Model 'llama3:8b' pulled successfully.${RESET}"
    else
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Local chat model 'llama3:8b' is ready.${RESET}"
    fi

    if ! ollama list | grep -q "all-minilm"; then
        echo -e "${YELLOW}WARNING:${RESET} ${WHITE}Model 'all-minilm' missing. Fetching model...${RESET}"
        ollama pull all-minilm >/dev/null 2>&1
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Model 'all-minilm' pulled successfully.${RESET}"
    else
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Local embedding model 'all-minilm' is ready.${RESET}"
    fi
}

check_docker() {
    step_header "4" "Deploying Auxiliary Docker Services"
    
    local DOCKER_COMPOSE_CMD=""
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    fi
    
    if [ -z "$DOCKER_COMPOSE_CMD" ]; then
        echo -e "${BG_RED} WARNING ${RESET} ${WHITE}docker compose not found. Auxiliary services will not launch.${RESET}"
    else
        echo -e "${CYAN}Spawning PGVector & MCP server...${RESET}"
        $DOCKER_COMPOSE_CMD -p orasaka -f ops/docker/docker-compose.yml up -d >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Containers deployed successfully.${RESET}"
        else
            echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Failed to launch Docker services.${RESET}"
        fi
    fi
}

start_sd_workers() {
    step_header "5" "Spawning Native Media Workers"
    
    RELATIVE_SD_SERVER="../../models/stable-diffusion/stable-diffusion.cpp/build/bin/sd-server"
    ABSOLUTE_SD_SERVER="$HOME/models/stable-diffusion/stable-diffusion.cpp/build/bin/sd-server"
    PID_FILE=".orasaka.pid"
    
    if [ -x "$ABSOLUTE_SD_SERVER" ]; then
      SD_SERVER_BIN="$ABSOLUTE_SD_SERVER"
    elif [ -x "$RELATIVE_SD_SERVER" ]; then
      SD_SERVER_BIN="$RELATIVE_SD_SERVER"
    else
      echo -e "${BG_RED} ERROR ${RESET} ${WHITE}sd-server binary not found.${RESET}" >&2
      exit 1
    fi
    
    MODEL_DIR="$HOME/models/stable-diffusion"
    IMAGE_MODEL="$MODEL_DIR/v1-5-pruned-emaonly.safetensors"
    VIDEO_MODEL="$MODEL_DIR/ltx-video-q4_k_m.safetensors"
    
    if [ ! -f "$IMAGE_MODEL" ]; then
      echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Image model not found at $IMAGE_MODEL${RESET}" >&2
      exit 1
    fi
    if [ ! -f "$VIDEO_MODEL" ]; then
      echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Video model not found at $VIDEO_MODEL${RESET}" >&2
      exit 1
    fi

    if lsof -i :8085 -t >/dev/null 2>&1; then
      echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Port 8085 is already in use.${RESET}" >&2
      exit 1
    fi
    if lsof -i :8086 -t >/dev/null 2>&1; then
      echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Port 8086 is already in use.${RESET}" >&2
      exit 1
    fi

    echo -e "${CYAN}Starting local Text-to-Image sd-server on Port 8085...${RESET}"
    "$SD_SERVER_BIN" --listen-port 8085 -m "$IMAGE_MODEL" > sd-image.log 2>&1 &
    IMAGE_PID=$!

    echo -e "${CYAN}Starting local Text-to-Video sd-server on Port 8086...${RESET}"
    "$SD_SERVER_BIN" --listen-port 8086 -m "$VIDEO_MODEL" > sd-video.log 2>&1 &
    VIDEO_PID=$!

    sleep 3 & loading_screen $!

    if ! lsof -i :8085 -t >/dev/null 2>&1; then
      echo -e "${YELLOW}WARNING:${RESET} Image server (PID $IMAGE_PID) did not bind to port 8085." >&2
    else
      echo -e "${GREEN}[ OK ]${RESET} Image server operational on port 8085."
    fi

    if ! lsof -i :8086 -t >/dev/null 2>&1; then
      echo -e "${YELLOW}WARNING:${RESET} Video server (PID $VIDEO_PID) did not bind to port 8086." >&2
    else
      echo -e "${GREEN}[ OK ]${RESET} Video server operational on port 8086."
    fi

    # Write state tracking file
    echo "# Orasaka Infrastructure PIDs" > "$PID_FILE"
    if [ -n "${OLLAMA_PID:-}" ]; then
      echo "OLLAMA_PID=$OLLAMA_PID" >> "$PID_FILE"
    fi
    echo "IMAGE_PID=$IMAGE_PID" >> "$PID_FILE"
    echo "VIDEO_PID=$VIDEO_PID" >> "$PID_FILE"
}

clean_caches() {
    echo -e "\n${CYAN}${BOLD}=== SYSTEM CLEANUP ===${RESET}"
    echo -e "${BLUE}Cleaning up local caches...${RESET}"
    rm -rf ./**/*.log ./orasaka-tools/target/*.log 2>/dev/null || true
    echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Local caches cleaned.${RESET}"
}

victory_screen() {
    echo ""
    echo -e "${BLUE}============================================================${RESET}"
    typewriter "🏆 BOOTSTRAP SUCCESSFUL 🏆" "${GREEN}${BOLD}"
    echo -e "${WHITE}Orasaka native inference ecosystem is fully operational.${RESET}"
    draw_gauge "SYSTEM_READY"
    echo -e "${CYAN}Execute modular Maven tasks to compile specific targets.${RESET}"
    echo -e "${CYAN}Example: 'mvn clean compile -pl orasaka-gateway'${RESET}"
    echo -e "${BLUE}============================================================${RESET}"
    echo ""
}

# --- Match Execution ---
check_java
check_ollama
check_ollama_models
check_docker
start_sd_workers
clean_caches
sleep 0.5
victory_screen

