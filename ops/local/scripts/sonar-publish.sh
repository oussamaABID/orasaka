#!/usr/bin/env bash
# ============================================================================
# ops/local/scripts/sonar-publish.sh
# Unified SonarCloud analysis for the entire Orasaka monorepo:
#   1. Java backend  — Maven build + JaCoCo coverage
#   2. orasaka-ui     — Next.js / TypeScript via sonar-scanner
#   3. orasaka-cli    — TypeScript CLI via sonar-scanner
#
# Usage:
#   ./ops/local/scripts/sonar-publish.sh            # Full monorepo scan
#   ./ops/local/scripts/sonar-publish.sh backend     # Java modules only
#   ./ops/local/scripts/sonar-publish.sh ui          # orasaka-ui only
#   ./ops/local/scripts/sonar-publish.sh cli         # orasaka-cli only
#
# Prerequisites:
#   - SONAR_TOKEN env var set (from .env or shell)
#   - Maven 3.9+ / Java 21
#   - npx sonar-scanner available
#   - jq + curl (for Quality Gate polling)
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# ── Defaults ─────────────────────────────────────────────────────────────────
SONAR_HOST="https://sonarcloud.io"
DEFAULT_ORG="oa"
DEFAULT_PROJECT_KEY="oussamaABID_orasaka"
QG_POLL_INTERVAL=5
QG_MAX_RETRIES=60

# ── Load .env if SONAR_TOKEN is not already set ──────────────────────────────
if [ -z "${SONAR_TOKEN:-}" ] && [ -f "$PROJECT_ROOT/.env" ]; then
  echo "📦 Loading SONAR variables from .env..."
  SONAR_TOKEN=$(grep -E '^SONAR_TOKEN=' "$PROJECT_ROOT/.env" | cut -d'=' -f2 | tr -d '"' | tr -d "'")
  SONAR_ORG=$(grep -E '^SONAR_ORG_KEY=' "$PROJECT_ROOT/.env" | cut -d'=' -f2 | tr -d '"' | tr -d "'" || true)
  SONAR_PROJECT_KEY=$(grep -E '^SONAR_PROJECT_KEY=' "$PROJECT_ROOT/.env" | cut -d'=' -f2 | tr -d '"' | tr -d "'" || true)
  export SONAR_TOKEN
fi

SONAR_ORG="${SONAR_ORG:-$DEFAULT_ORG}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-$DEFAULT_PROJECT_KEY}"

if [ -z "${SONAR_TOKEN:-}" ]; then
  echo "❌ SONAR_TOKEN is not set. Export it or add it to .env"
  exit 1
fi

# ── Detect current git branch ────────────────────────────────────────────────
BRANCH_NAME="${SONAR_BRANCH_NAME:-$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'main')}"

echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo "  🎯 Orasaka SonarCloud Monorepo Analysis"
echo "═══════════════════════════════════════════════════════════════════════"
echo "  Organization : $SONAR_ORG"
echo "  Project Key  : $SONAR_PROJECT_KEY"
echo "  Branch       : $BRANCH_NAME"
echo "  Host         : $SONAR_HOST"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# PHASE 1: Build & Coverage
# ─────────────────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════════════"
echo "  🛠️  PHASE 1 — Building and collecting coverage for all modules"
echo "═══════════════════════════════════════════════════════════════════════"

echo "☕ Building Java backend + JaCoCo coverage..."
cd "$PROJECT_ROOT"
mvn clean test jacoco:report dependency:copy-dependencies -DincludeScope=runtime -DskipTests=false -q || true

echo "⚛️  Testing orasaka-ui..."
cd "$PROJECT_ROOT/orasaka-ui"
npm run test:coverage 2>/dev/null || true

echo "🖥️  Testing orasaka-cli..."
cd "$PROJECT_ROOT/orasaka-cli"
npm run test:coverage 2>/dev/null || true

# ─────────────────────────────────────────────────────────────────────────────
# PHASE 2: Unified SonarCloud Analysis
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo "  🚀 PHASE 2 — Unified Monorepo SonarCloud Scan"
echo "═══════════════════════════════════════════════════════════════════════"
cd "$PROJECT_ROOT"

npx -y sonar-scanner \
  -Dsonar.organization="$SONAR_ORG" \
  -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.host.url="$SONAR_HOST" \
  -Dsonar.branch.name="$BRANCH_NAME" \
  -Dsonar.typescript.lcov.reportPaths="orasaka-ui/coverage/lcov.info,orasaka-cli/coverage/lcov.info" \
  -Dsonar.javascript.lcov.reportPaths="orasaka-ui/coverage/lcov.info,orasaka-cli/coverage/lcov.info"

# ─────────────────────────────────────────────────────────────────────────────
# PHASE 3: Quality Gate Polling
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo "  🔍 PHASE 3 — Polling Quality Gate result"
echo "═══════════════════════════════════════════════════════════════════════"

STATUS="PENDING"
RETRIES=0

while [ "$STATUS" != "OK" ] && [ "$STATUS" != "ERROR" ]; do
  if [ "$RETRIES" -ge "$QG_MAX_RETRIES" ]; then
    echo "⏱️  Quality Gate polling timed out after $((QG_MAX_RETRIES * QG_POLL_INTERVAL))s"
    exit 1
  fi

  sleep "$QG_POLL_INTERVAL"
  RETRIES=$((RETRIES + 1))

  RESPONSE=$(curl -s -u "$SONAR_TOKEN": \
    "$SONAR_HOST/api/qualitygates/project_status?projectKey=$SONAR_PROJECT_KEY&branch=$BRANCH_NAME")

  STATUS=$(echo "$RESPONSE" | jq -r '.projectStatus.status' 2>/dev/null)

  if [ "$STATUS" = "null" ] || [ -z "$STATUS" ]; then
    echo "  ⏳ Still processing... (attempt $RETRIES/$QG_MAX_RETRIES)"
    STATUS="PENDING"
  else
    echo "  📊 Status: $STATUS (attempt $RETRIES)"
  fi
done

echo ""
if [ "$STATUS" = "ERROR" ]; then
  echo "═══════════════════════════════════════════════════════════════════════"
  echo "  ❌ Quality Gate FAILED"
  echo "═══════════════════════════════════════════════════════════════════════"
  echo ""
  echo "  Failing conditions:"
  echo "$RESPONSE" | jq -c '.projectStatus.conditions[] | select(.status == "ERROR")' 2>/dev/null | while read -r condition; do
    METRIC=$(echo "$condition" | jq -r '.metricKey')
    ACTUAL=$(echo "$condition" | jq -r '.actualValue')
    THRESHOLD=$(echo "$condition" | jq -r '.errorThreshold')
    echo "    ⚠️  $METRIC: $ACTUAL (threshold: $THRESHOLD)"
  done
  echo ""
  echo "  📊 Dashboard: $SONAR_HOST/dashboard?id=$SONAR_PROJECT_KEY&branch=$BRANCH_NAME"
  echo "═══════════════════════════════════════════════════════════════════════"
  exit 1
else
  echo "═══════════════════════════════════════════════════════════════════════"
  echo "  ✅ Quality Gate PASSED"
  echo "═══════════════════════════════════════════════════════════════════════"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo "  🎯 SonarCloud analysis complete!"
echo "  📊 Dashboard: $SONAR_HOST/dashboard?id=$SONAR_PROJECT_KEY&branch=$BRANCH_NAME"
echo "═══════════════════════════════════════════════════════════════════════"
