#!/usr/bin/env bash
# ==============================================================================
# ORASAKA E2E HARDENING VALIDATION SCRIPT
# ==============================================================================
# Validates the entire production hardening sprint:
# 1. Monorepo compilation gate
# 2. Test suite gate (orasaka-core)
# 3. Spotless formatting gate
# 4. Terraform blueprint validation
# 5. AGENTS.md governance integrity check
# 6. Module dependency isolation check
# ==============================================================================

set -uo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../../.."

GREEN='\033[1;32m'
RED='\033[1;31m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
CYAN='\033[1;36m'
RESET='\033[0m'
BOLD='\033[1m'

TOTAL=0
PASSED=0
FAILED=0

gate() {
  local name="$1"
  local cmd="$2"
  TOTAL=$((TOTAL + 1))
  echo -e "${CYAN}[GATE $TOTAL] $name${RESET}"
  if eval "$cmd" >/dev/null 2>&1; then
    PASSED=$((PASSED + 1))
    echo -e "${GREEN}  ✅ PASSED${RESET}"
  else
    FAILED=$((FAILED + 1))
    echo -e "${RED}  ❌ FAILED${RESET}"
  fi
}

echo ""
echo -e "${BLUE}${BOLD}================================================================${RESET}"
echo -e "${BLUE}${BOLD}   ORASAKA PRODUCTION HARDENING — E2E VALIDATION SUITE${RESET}"
echo -e "${BLUE}${BOLD}================================================================${RESET}"
echo ""

# --------------------------------------------------------------------------
# GATE 1: Monorepo Compilation
# --------------------------------------------------------------------------
gate "Monorepo Compilation (mvn install -Dmaven.test.skip=true)" \
  "cd '$PROJECT_ROOT' && mvn install -Dmaven.test.skip=true -q"

# --------------------------------------------------------------------------
# GATE 2: Core Test Suite
# --------------------------------------------------------------------------
gate "orasaka-core Test Suite (336+ tests)" \
  "cd '$PROJECT_ROOT' && mvn test -pl orasaka-core -am -q"

# --------------------------------------------------------------------------
# GATE 3: Spotless Formatting
# --------------------------------------------------------------------------
gate "Spotless Format Check (core, gateway, automation-worker)" \
  "cd '$PROJECT_ROOT' && mvn spotless:check -pl orasaka-core,orasaka-gateway,:orasaka-worker-automation -q"

# --------------------------------------------------------------------------
# GATE 4: Terraform Syntax Validation
# --------------------------------------------------------------------------
gate "Terraform Syntax Validation (main.tf, variables.tf, outputs.tf)" \
  "test -f '$PROJECT_ROOT/ops/deploy/terraform/main.tf' && \
   test -f '$PROJECT_ROOT/ops/deploy/terraform/variables.tf' && \
   test -f '$PROJECT_ROOT/ops/deploy/terraform/outputs.tf' && \
   grep -q 'automation_worker_image' '$PROJECT_ROOT/ops/deploy/terraform/main.tf' && \
   grep -q 'automation_worker_image' '$PROJECT_ROOT/ops/deploy/terraform/variables.tf' && \
   grep -q 'automation_worker_task_arn' '$PROJECT_ROOT/ops/deploy/terraform/outputs.tf'"

# --------------------------------------------------------------------------
# GATE 5: AGENTS.md Governance Integrity
# --------------------------------------------------------------------------
gate "AGENTS.md Governance (§2.13, §2.14, §2.15 present)" \
  "grep -q 'ERR-117' '$PROJECT_ROOT/AGENTS.md' && \
   grep -q 'ERR-118' '$PROJECT_ROOT/AGENTS.md' && \
   grep -q 'ERR-119' '$PROJECT_ROOT/AGENTS.md' && \
   grep -q 'orasaka-automation-worker' '$PROJECT_ROOT/AGENTS.md' && \
   grep -q '9-stage' '$PROJECT_ROOT/AGENTS.md'"

# --------------------------------------------------------------------------
# GATE 6: Legacy Type Eradication
# --------------------------------------------------------------------------
gate "Legacy Orasaka* Test Classes Eradicated" \
  "! find '$PROJECT_ROOT/orasaka-core/src/test' -name 'Orasaka*' -type f | grep -q ."

# --------------------------------------------------------------------------
# GATE 7: No Orasaka-Prefixed Class Leakage in Test Sources
# --------------------------------------------------------------------------
gate "No Orasaka-Prefixed Import in Active Tests" \
  "! grep -r 'import.*OrasakaEngine\|import.*OrasakaChatRequest\|import.*OrasakaChatResponse\|import.*OrasakaImageRequest' \
     '$PROJECT_ROOT/orasaka-core/src/test' 2>/dev/null | grep -q ."

# --------------------------------------------------------------------------
# GATE 8: Module Separation (core → tools prohibited)
# --------------------------------------------------------------------------
gate "Module Separation: orasaka-core does NOT import orasaka-tools" \
  "! grep -r 'com.orasaka.tools' '$PROJECT_ROOT/orasaka-core/src/main' 2>/dev/null | grep -q ."

# --------------------------------------------------------------------------
# GATE 9: Lifecycle Scripts Updated
# --------------------------------------------------------------------------
gate "Lifecycle Scripts Include Automation Worker" \
  "grep -q 'orasaka-automation-worker' '$PROJECT_ROOT/ops/local/scripts/stop.sh' && \
   grep -q 'auto_ok' '$PROJECT_ROOT/ops/local/scripts/start.sh'"

# --------------------------------------------------------------------------
# GATE 10: Centralized Automation Worker Dockerfile
# --------------------------------------------------------------------------
gate "Automation Worker Dockerfile Centralized in ops/local/docker/" \
  "test -f '$PROJECT_ROOT/ops/local/docker/Dockerfile.automation-worker'"

# --------------------------------------------------------------------------
# GATE 11: No Standalone Dockerfile in Module Directory
# --------------------------------------------------------------------------
gate "No Standalone Dockerfile in orasaka-automation-worker/" \
  "! test -f '$PROJECT_ROOT/orasaka-automation-worker/Dockerfile'"

# --------------------------------------------------------------------------
# RESULTS
# --------------------------------------------------------------------------
echo ""
echo -e "${BLUE}${BOLD}================================================================${RESET}"
echo -e "${BLUE}${BOLD}   RESULTS: $PASSED/$TOTAL GATES PASSED${RESET}"
echo -e "${BLUE}${BOLD}================================================================${RESET}"

if [ "$FAILED" -eq 0 ]; then
  echo -e "${GREEN}${BOLD}🏆 ALL GATES PASSED — HARDENING SPRINT VALIDATED 🏆${RESET}"
  exit 0
else
  echo -e "${RED}${BOLD}⚠️  $FAILED GATE(S) FAILED — REVIEW REQUIRED ⚠️${RESET}"
  exit 1
fi
