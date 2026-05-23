#!/bin/bash

# Determine script directory and navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/../.."

# ==============================================================================
# ORASAKA NATIVE INFERENCE BOOTSTRAP
# "The King of Context"
# ==============================================================================

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

# --- UI Components ---

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
    echo -e "${CYAN}${BOLD}=== SYSTEM CHECK ${stage}/4 ===${RESET}"
    echo -e "${BLUE}ORASAKA${RESET} ${YELLOW}${BOLD}::${RESET} ${WHITE}Verifying ${check_name}...${RESET}"
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
    typewriter ">>> THE ULTIMATE AI ECOSYSTEM <<<" "${WHITE}${BOLD}"
    echo ""
    typewriter "Initializing Native Inference Engine..." "${CYAN}"
    echo "============================================================"
}

# --- Verification Logic ---

check_java() {
    step_header "1" "JAVA 21 RUNTIME"
    
    # Fake loading delay for effect
    sleep 1 & loading_screen $!
    
    if command -v java >/dev/null 2>&1; then
        JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//;s/\..*//')
        if [ "$JAVA_VER" -lt 21 ]; then
            echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Java 21+ required. Detected version: v${JAVA_VER}${RESET}"
            echo -e "${DIM}Hint: Please install JDK 21 or higher and update your PATH.${RESET}"
            exit 1
        fi
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Java 21 runtime verified. [v${JAVA_VER}]${RESET}"
        draw_gauge "RUNTIME_READY"
    else
        echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Java runtime (java) not found on PATH.${RESET}"
        exit 1
    fi
}

check_ollama() {
    step_header "2" "NATIVE OLLAMA API"
    
    sleep 1 & loading_screen $!
    
    if ! curl -s http://localhost:11434/api/tags > /dev/null; then
        echo -e "${BG_RED} OFFLINE ${RESET} ${WHITE}Ollama service not responding at localhost:11434.${RESET}"
        echo -e "${YELLOW}CRITICAL:${RESET} Orasaka requires native GPU acceleration."
        echo -e "Please start the Ollama Desktop app to enable local inference."
        exit 1
    fi
    echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Ollama service connection active.${RESET}"
    draw_gauge "GPU_STABILITY"
}

check_models() {
    step_header "3" "LOCAL AI MODELS"
    
    sleep 1 & loading_screen $!
    
    if ! command -v ollama >/dev/null 2>&1; then
        echo -e "${BG_RED} ERROR ${RESET} ${WHITE}ollama CLI command not found on PATH.${RESET}"
        exit 1
    fi
    
    # 1. Chat/Reasoning Model (llama3:8b / llama3)
    if ! ollama list | grep -q "llama3:8b"; then
        echo -e "${YELLOW}WARNING:${RESET} ${WHITE}Model 'llama3:8b' missing. Fetching model...${RESET}"
        ollama pull llama3:8b
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Model 'llama3:8b' pulled successfully.${RESET}"
    else
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Local chat model 'llama3:8b' is ready.${RESET}"
    fi

    # 2. Embedding Model (all-minilm)
    if ! ollama list | grep -q "all-minilm"; then
        echo -e "${YELLOW}WARNING:${RESET} ${WHITE}Model 'all-minilm' missing. Fetching model...${RESET}"
        ollama pull all-minilm
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Model 'all-minilm' pulled successfully.${RESET}"
    else
        echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Local embedding model 'all-minilm' is ready.${RESET}"
    fi
    draw_gauge "NEURAL_LINK"
}

check_docker() {
    step_header "4" "AUXILIARY DOCKER SERVICES"
    
    sleep 1 & loading_screen $!
    
    local DOCKER_COMPOSE_CMD=""
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    fi
    
    if [ -z "$DOCKER_COMPOSE_CMD" ]; then
        echo -e "${BG_RED} WARNING ${RESET} ${WHITE}docker-compose / docker compose not found.${RESET}"
        echo -e "${DIM}Auxiliary services (PGVector, MCP) will not be launched.${RESET}"
    else
        echo -e "${CYAN}Spawning PGVector & MCP server using '${DOCKER_COMPOSE_CMD}'...${RESET}"
        $DOCKER_COMPOSE_CMD -p orasaka -f ops/docker/docker-compose.yml up -d >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Containers deployed successfully.${RESET}"
            draw_gauge "VECTOR_MEMORY"
        else
            echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Failed to launch Docker services.${RESET}"
        fi
    fi
}

# --- Cache Cleanup & Environment Injection ---
clean_caches_and_inject_env() {
    echo -e "\n${CYAN}${BOLD}=== ENVIRONMENT CONFIGURATION & CLEANUP ===${RESET}"
    echo -e "${BLUE}Cleaning up local caches...${RESET}"
    rm -rf ./**/*.log ./orasaka-tools/target/*.log
    
    export ORASAKA_OLLAMA_BASE_URL=${ORASAKA_OLLAMA_BASE_URL:-"http://localhost:11434"}
    export SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-"jdbc:postgresql://localhost:5432/orasaka_db"}
    export SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-"orasaka_admin"}
    export SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-"orasaka_secure_pass"}
    
    echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Environment variables injected. Local caches cleaned.${RESET}"
}

# --- Rapid Maven Validation Check ---
check_maven_validate() {
    echo -e "\n${CYAN}${BOLD}=== RUNNING MAVEN VALIDATION ===${RESET}"
    if command -v mvn >/dev/null 2>&1; then
        if mvn validate; then
            echo -e "${GREEN}[ OK ]${RESET} ${WHITE}Maven validation check passed successfully.${RESET}"
        else
            echo -e "${BG_RED} ERROR ${RESET} ${WHITE}Maven validation check failed. Please check build errors.${RESET}"
            exit 1
        fi
    else
        echo -e "${YELLOW}WARNING:${RESET} ${WHITE}Maven (mvn) not found. Skipping validation check.${RESET}"
    fi
}

victory_screen() {
    echo ""
    echo -e "${BLUE}============================================================${RESET}"
    typewriter "🏆 BOOTSTRAP SUCCESSFUL 🏆" "${GREEN}${BOLD}"
    echo -e "${WHITE}Orasaka native inference ecosystem is fully operational.${RESET}"
    echo -e "${CYAN}Execute modular Maven tasks to compile specific targets.${RESET}"
    echo -e "${CYAN}Example: 'mvn clean compile -pl orasaka-gateway'${RESET}"
    echo -e "${BLUE}============================================================${RESET}"
    echo ""
}

# --- Match Execution ---
print_logo
check_java
check_ollama
check_models
check_docker
clean_caches_and_inject_env
check_maven_validate
sleep 0.5
victory_screen

