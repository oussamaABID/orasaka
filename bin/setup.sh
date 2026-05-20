#!/bin/bash

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

# Handle Rage Quits (Ctrl+C)
trap 'echo -e "\n${BG_RED} FATALITY! ${RESET} ${RED}Player triggered RAGE QUIT. Match aborted.${RESET}\n"; exit 1' INT

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
    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf " [%c]  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

versus_screen() {
    local stage="$1"
    local challenger="$2"
    echo ""
    echo -e "${CYAN}${BOLD}=== STAGE ${stage} ===${RESET}"
    echo -e "${BLUE}ORASAKA${RESET} ${YELLOW}${BOLD}VS.${RESET} ${RED}${challenger}${RESET}"
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
    typewriter "CREDIT 1  -  INSERT COIN" "${YELLOW}"
    sleep 0.3
    typewriter "Initializing Native Inference Engine..." "${CYAN}"
    echo "============================================================"
}

# --- Battle Logic (Checks) ---

check_java() {
    versus_screen "1" "JAVA 21"
    
    # Fake loading delay for effect
    sleep 1 & loading_screen $!
    
    if command -v java >/dev/null 2>&1; then
        JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//;s/\..*//')
        if [ "$JAVA_VER" -lt 21 ]; then
            echo -e "${BG_RED} K.O. ${RESET} ${WHITE}Java 21+ required. Detected: v${JAVA_VER}${RESET}"
            echo -e "${DIM}Hint: Update your JDK stance and try again.${RESET}"
            exit 1
        fi
        echo -e "${GREEN}PERFECT!${RESET} ${WHITE}Execution engine mounted. [v${JAVA_VER}]${RESET}"
        draw_gauge "RUNTIME_POWER"
    else
        echo -e "${BG_RED} K.O. ${RESET} ${WHITE}Java compiler missing from the arena.${RESET}"
        exit 1
    fi
}

check_ollama() {
    versus_screen "2" "NATIVE OLLAMA"
    
    sleep 1 & loading_screen $!
    
    if ! curl -s http://localhost:11434/api/tags > /dev/null; then
        echo -e "${BG_RED} TIME OVER ${RESET} ${WHITE}Ollama service not responding.${RESET}"
        echo -e "${YELLOW}CRITICAL:${RESET} Orasaka requires native GPU acceleration."
        echo -e "Please start the Ollama Desktop app to unleash your ${MAGENTA}Desperation Move${RESET}."
        exit 1
    fi
    echo -e "${GREEN}C-C-C-COMBO!${RESET} ${WHITE}Ollama connection secured.${RESET}"
    draw_gauge "GPU_ACCELERATION"
}

check_models() {
    versus_screen "3" "LLAMA3 WEIGHTS"
    
    sleep 1 & loading_screen $!
    
    if ! command -v ollama >/dev/null 2>&1; then
        echo -e "${BG_RED} FATAL COUNTER ${RESET} ${WHITE}ollama CLI dropped from combo.${RESET}"
        exit 1
    fi
    
    if ! ollama list | grep -q "llama3"; then
        echo -e "${YELLOW}WARNING:${RESET} ${WHITE}Challenger 'llama3' missing. Pulling into the ring...${RESET}"
        ollama pull llama3
        echo -e "${GREEN}REVERSAL!${RESET} ${WHITE}Model successfully downloaded.${RESET}"
    else
        echo -e "${GREEN}SUPER ART!${RESET} ${WHITE}Local model 'llama3' is ready to brawl.${RESET}"
    fi
    draw_gauge "NEURAL_LINK"
}

check_docker() {
    versus_screen "FINAL" "AUXILIARY SERVICES"
    
    sleep 1 & loading_screen $!
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${BG_RED} GUARD CRUSH ${RESET} ${WHITE}docker-compose not found.${RESET}"
        echo -e "${DIM}Assist characters (PGVector, MCP) will sit this match out.${RESET}"
    else
        echo -e "${CYAN}Tagging in PGVector & MCP server...${RESET}"
        docker-compose up -d >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}DOUBLE TEAM!${RESET} ${WHITE}Containers deployed successfully.${RESET}"
            draw_gauge "VECTOR_MEMORY"
        else
            echo -e "${BG_RED} COUNTER HIT ${RESET} ${WHITE}Failed to tag in Docker services.${RESET}"
        fi
    fi
}

victory_screen() {
    echo ""
    echo -e "${BLUE}============================================================${RESET}"
    typewriter "🏆 YOU WIN! 🏆" "${GREEN}${BOLD}"
    echo -e "${WHITE}Orasaka native inference ecosystem is fully operational.${RESET}"
    echo -e "${CYAN}Execute 'mvn clean install' to complete your run.${RESET}"
    echo -e "${BLUE}============================================================${RESET}"
    echo ""
}

# --- Match Execution ---
print_logo
check_java
check_ollama
check_models
check_docker
sleep 0.5
victory_screen
