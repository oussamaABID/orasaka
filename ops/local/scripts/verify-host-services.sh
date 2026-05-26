#!/usr/bin/env bash
# ops/local/scripts/verify-host-services.sh
# Verifies host-level AI engines (Ollama, LocalAI, SVD Video Worker, sd-server)

# Ensure script is sourced, not executed directly, or handle both gracefully.
if [ -z "${PROJECT_ROOT:-}" ]; then
  SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"
  PID_FILE="$PROJECT_ROOT/var/.orasaka.pid"
  
  # Load env if needed
  ENV_FILE="$PROJECT_ROOT/.env"
  if [ -f "$ENV_FILE" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
      if [[ ! "$line" =~ ^[[:space:]]*# ]] && [[ -n "$line" ]]; then
        clean_line=$(echo "$line" | tr -d '\r')
        export "$clean_line"
      fi
    done < "$ENV_FILE"
  fi
  
  GREEN='\033[1;32m'
  RED='\033[1;31m'
  YELLOW='\033[1;33m'
  BLUE='\033[1;34m'
  CYAN='\033[1;36m'
  WHITE='\033[1;37m'
  RESET='\033[0m'
fi

# A) OLLAMA (Port 11434)
echo -e "${CYAN}Checking Ollama Gateway on port 11434...${RESET}"
if ! curl -s http://localhost:11434/ >/dev/null 2>&1; then
    echo -e "${YELLOW}[ WARN ]${RESET} Ollama is not active on host. Attempting to start...${RESET}"
    if open -a Ollama &>/dev/null; then
        echo -e "${CYAN}Triggered Ollama application via macOS open command.${RESET}"
    else
        echo -e "${CYAN}Attempting background launch via 'ollama serve'...${RESET}"
        if command -v ollama &>/dev/null; then
            ollama serve >/dev/null 2>&1 &
        else
            echo -e "${RED}[ ERROR ]${RESET} Ollama command not found on host."
            exit 1
        fi
    fi
    # Wait for Ollama to become responsive
    retries=30
    success=false
    while [ $retries -gt 0 ]; do
        if curl -s http://localhost:11434/ >/dev/null 2>&1; then
            success=true
            break
        fi
        echo -ne "."
        sleep 1
        retries=$((retries - 1))
    done
    if [ "$success" = false ]; then
        echo -e "\n${RED}[ ERROR ]${RESET} Ollama server failed to respond on port 11434."
        exit 1
    fi
    echo -e "\n${GREEN}[ OK ]${RESET} Ollama is now responsive."
else
    echo -e "${GREEN}[ OK ]${RESET} Ollama is already active on host."
    echo -e "${YELLOW}[ NOTE ]${RESET} Enforced memory parameters: OLLAMA_NUM_PARALLEL=${OLLAMA_NUM_PARALLEL:-1}, OLLAMA_KEEP_ALIVE=${OLLAMA_KEEP_ALIVE:-"24h"}"
fi

# Verify 'phi3:mini' is present
echo -e "${CYAN}Verifying 'phi3:mini' model...${RESET}"
if ! command -v ollama &>/dev/null; then
    echo -e "${RED}[ ERROR ]${RESET} ollama command line tool is missing on PATH."
    exit 1
fi
if ! (ollama list 2>/dev/null | grep -q "phi3:mini" || curl -s http://localhost:11434/api/tags | grep -q "phi3:mini"); then
    echo -e "${YELLOW}[ WARN ]${RESET} phi3:mini model is missing. Pulling phi3:mini..."
    ollama pull phi3:mini
fi
echo -e "${GREEN}[ OK ]${RESET} phi3:mini is ready."

# B) IMAGE & AUDIO WORKER (Port 8085 - LocalAI)
echo -e "${CYAN}Checking Image & Audio Worker on port 8085...${RESET}"
if ! curl -s http://localhost:8085/ >/dev/null 2>&1; then
    echo -e "${YELLOW}[ WARN ]${RESET} LocalAI on port 8085 is not active. Attempting to start...${RESET}"
    
    mkdir -p "$PROJECT_ROOT/var/logs"
    
    LOCALAI_BIN=""
    if command -v local-ai &>/dev/null; then
        LOCALAI_BIN="local-ai"
    elif command -v localai &>/dev/null; then
        LOCALAI_BIN="localai"
    elif [ -f "$PROJECT_ROOT/ops/local/bin/local-ai" ]; then
        LOCALAI_BIN="$PROJECT_ROOT/ops/local/bin/local-ai"
    elif [ -f "$PROJECT_ROOT/ops/local/bin/localai" ]; then
        LOCALAI_BIN="$PROJECT_ROOT/ops/local/bin/localai"
    fi

    if [ -z "$LOCALAI_BIN" ]; then
        echo -e "${RED}[ ERROR ]${RESET} LocalAI binary not found. Please run ops/local/install-tooling.sh first."
        exit 1
    fi
    
    echo -e "${CYAN}Starting native LocalAI on port 8085 from $LOCALAI_BIN...${RESET}"
    DATE_STAMP=$(date +%Y%m%d)
    IMAGE_LOG="$PROJECT_ROOT/var/logs/${DATE_STAMP}_image-worker.log"
    mkdir -p "$(dirname "$IMAGE_LOG")"
    "$LOCALAI_BIN" --models-path "$HOME/models/stable-diffusion" --backends-path "$HOME/models/stable-diffusion/backends" --address "127.0.0.1:8085" 2>&1 | /usr/sbin/rotatelogs -n 5 "$IMAGE_LOG" 1M &
    IMAGE_PID=$!
    
    echo "image-worker=$IMAGE_PID" >> "$PID_FILE"
    
    sleep 3
    if ! ps -p "$IMAGE_PID" >/dev/null 2>&1; then
        echo -e "${RED}[ ERROR ]${RESET} LocalAI process died immediately after startup. Check logs in var/logs/image-worker.log"
        exit 1
    fi
    
    retries=15
    success=false
    while [ $retries -gt 0 ]; do
        if curl -s http://localhost:8085/ >/dev/null 2>&1; then
            success=true
            break
        fi
        echo -ne "."
        sleep 2
        retries=$((retries - 1))
    done
    if [ "$success" = false ]; then
        echo -e "\n${RED}[ ERROR ]${RESET} LocalAI failed to respond on port 8085."
        exit 1
    fi
    echo -e "\n${GREEN}[ OK ]${RESET} LocalAI is now responsive."
fi

# C) VIDEO WORKER (Port 8188)
echo -e "${CYAN}Checking Video Worker on port 8188...${RESET}"
if ! curl -s http://localhost:8188/ >/dev/null 2>&1; then
    echo -e "${YELLOW}[ WARN ]${RESET} Video Worker is not active. Starting native video worker...${RESET}"
    
    mkdir -p "$PROJECT_ROOT/var/logs"
    
    SD_VIDEO_MODEL="$HOME/models/stable-diffusion/svd_xt.safetensors"
    if [ ! -f "$SD_VIDEO_MODEL" ]; then
        echo -e "${RED}[ ERROR ]${RESET} Stable Video Diffusion model file missing at '$SD_VIDEO_MODEL'. Run ops/local/install-tooling.sh first."
        exit 1
    fi
    
    echo -e "${CYAN}Starting native SVD video worker on port 8188...${RESET}"
    DATE_STAMP=$(date +%Y%m%d)
    VIDEO_LOG="$PROJECT_ROOT/var/logs/${DATE_STAMP}_video-worker.log"
    mkdir -p "$(dirname "$VIDEO_LOG")"
    PYTHONPATH="$PROJECT_ROOT/orasaka-workers/video" python3 -u "$PROJECT_ROOT/orasaka-workers/video/app/main.py" 2>&1 | /usr/sbin/rotatelogs -n 5 "$VIDEO_LOG" 1M &
    VIDEO_PID=$!
    
    echo "video-worker=$VIDEO_PID" >> "$PID_FILE"
    
    sleep 3
    if ! ps -p "$VIDEO_PID" >/dev/null 2>&1; then
        echo -e "${RED}[ ERROR ]${RESET} Video Worker process died immediately after startup. Check logs in var/logs/video-worker.log"
        exit 1
    fi
    
    retries=15
    success=false
    while [ $retries -gt 0 ]; do
        if curl -s http://localhost:8188/ >/dev/null 2>&1; then
            success=true
            break
        fi
        echo -ne "."
        sleep 2
        retries=$((retries - 1))
    done
    if [ "$success" = false ]; then
        echo -e "\n${RED}[ ERROR ]${RESET} Video Worker failed to respond on port 8188."
        exit 1
    fi
    echo -e "\n${GREEN}[ OK ]${RESET} Video Worker is now responsive."
fi

# D) IMAGE GENERATION WORKER (Port 8086 - sd-server)
echo -e "${CYAN}Checking Image Generation Worker on port 8086...${RESET}"
if ! curl -s http://localhost:8086/ >/dev/null 2>&1; then
    echo -e "${YELLOW}[ WARN ]${RESET} Image Generation Worker is not active. Starting native sd-server...${RESET}"
    
    mkdir -p "$PROJECT_ROOT/var/logs"
    
    SD_MODEL="$HOME/models/stable-diffusion/v1-5-pruned-emaonly.safetensors"
    if [ ! -f "$SD_MODEL" ]; then
        echo -e "${RED}[ ERROR ]${RESET} Stable Diffusion model file missing at '$SD_MODEL'. Run ops/local/install-tooling.sh first."
        exit 1
    fi
    
    SD_SERVER_BIN="$HOME/models/stable-diffusion/stable-diffusion.cpp/build/bin/sd-server"
    if [ ! -f "$SD_SERVER_BIN" ]; then
        echo -e "${RED}[ ERROR ]${RESET} sd-server missing at '$SD_SERVER_BIN'. Please build stable-diffusion.cpp first."
        exit 1
    fi
    
    echo -e "${CYAN}Starting native sd-server on port 8086...${RESET}"
    DATE_STAMP=$(date +%Y%m%d)
    IMAGE_GEN_LOG="$PROJECT_ROOT/var/logs/${DATE_STAMP}_image-generation-worker.log"
    mkdir -p "$(dirname "$IMAGE_GEN_LOG")"
    "$SD_SERVER_BIN" --listen-port 8086 -m "$SD_MODEL" --seed "${IMAGE_GEN_SEED:--1}" 2>&1 | /usr/sbin/rotatelogs -n 5 "$IMAGE_GEN_LOG" 1M &
    IMAGE_GEN_PID=$!
    
    echo "image-gen-worker=$IMAGE_GEN_PID" >> "$PID_FILE"
    
    sleep 3
    if ! ps -p "$IMAGE_GEN_PID" >/dev/null 2>&1; then
        echo -e "${RED}[ ERROR ]${RESET} Image Generation Worker process died immediately after startup. Check logs"
        exit 1
    fi
    
    retries=15
    success=false
    while [ $retries -gt 0 ]; do
        if curl -s http://localhost:8086/ >/dev/null 2>&1; then
            success=true
            break
        fi
        echo -ne "."
        sleep 2
        retries=$((retries - 1))
    done
    if [ "$success" = false ]; then
        echo -e "\n${RED}[ ERROR ]${RESET} Image Generation Worker failed to respond on port 8086."
        exit 1
    fi
    echo -e "\n${GREEN}[ OK ]${RESET} Image Generation Worker is now responsive."
fi
