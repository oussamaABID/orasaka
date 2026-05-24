#!/usr/bin/env bash

# Determine script directory and navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/../.."

# ==============================================================================
# ORASAKA LOCAL INFRASTRUCTURE KILLSWITCH PURGE ENGINE
# ==============================================================================
# Gracefully terminates all active Ollama and sd-server background tasks
# ==============================================================================

set -uo pipefail

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

echo ""
echo -e "${MAGENTA}${BOLD}=== ORASAKA KILLSWITCH PURGE ENGINE ===${RESET}"
echo ""

PID_FILE=".orasaka.pid"

terminate_process() {
  local pid=$1
  local name=$2

  if [ -n "$pid" ] && ps -p "$pid" >/dev/null 2>&1; then
    echo -e "${CYAN}Terminating $name (PID $pid) gracefully...${RESET}"
    kill -15 "$pid" 2>/dev/null
    # Wait for up to 5 seconds
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
    # Skip comments and empty lines
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
else
  echo -e "${YELLOW}WARNING:${RESET} State file $PID_FILE not found. Triggering automated socket-inspection fallback..."
fi

# 2. Fallback Guard: Socket Inspection for ports 8085 and 8086
for port in 8085 8086; do
  echo -e "${DIM}Inspecting Port $port...${RESET}"
  pids=$(lsof -i :$port -t 2>/dev/null || true)
  if [ -n "$pids" ]; then
    for pid in $pids; do
      if [ -n "$pid" ]; then
        terminate_process "$pid" "Rogue process on Port $port"
      fi
    done
  fi
done

# Clear state file again to ensure fresh recycling
rm -f "$PID_FILE"

echo ""
typewriter "🛑 TEARDOWN COMPLETE 🛑" "${GREEN}${BOLD}"
echo -e "${WHITE}All memory pools and model weights recycled cleanly.${RESET}"
echo ""
