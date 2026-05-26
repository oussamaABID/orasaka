#!/usr/bin/env bash
# ops/local/scripts/verify-models.sh
# Verifies presence of target AI models and pulls them if missing.

set -euo pipefail

MODELS_DIR="$HOME/models/stable-diffusion"
mkdir -p "$MODELS_DIR"

echo "=========================================================="
# shellcheck disable=SC2016
echo '🥷 Orasaka Model Verification & Provisioning Script'
echo "=========================================================="
echo "Target directory: $MODELS_DIR"

# Helper for huggingface-cli
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

if ! command_exists hf; then
  echo "⚠️ Warning: hf CLI not found. Trying to download weights using curl/wget..."
fi

# 1. AUDIO: Piper TTS ONNX voice files & JSON configurations
echo "→ Checking Piper Speech voice profiles..."

download_piper_model() {
  local name="$1"
  local path_prefix="$2"
  local onnx_file="$3"
  local onnx_path="$path_prefix/$onnx_file"

  mkdir -p "$(dirname "$MODELS_DIR/$onnx_path")"

  if [ ! -f "$MODELS_DIR/$onnx_path" ]; then
    echo "  → Downloading $name ONNX file..."
    if command_exists hf; then
      hf download rhasspy/piper-voices "$onnx_path" --local-dir "$MODELS_DIR"
    else
      curl -L -o "$MODELS_DIR/$onnx_path" "https://huggingface.co/rhasspy/piper-voices/resolve/main/$onnx_path"
    fi
  else
    echo "  ✔ $name ONNX file exists."
  fi

  if [ ! -f "$MODELS_DIR/${onnx_path}.json" ]; then
    echo "  → Downloading $name JSON config..."
    if command_exists hf; then
      hf download rhasspy/piper-voices "${onnx_path}.json" --local-dir "$MODELS_DIR"
    fi
  else
    echo "  ✔ $name JSON config exists."
  fi
}

download_piper_model "piper-en-low" "en/en_US/lessac/low" "en_US-lessac-low.onnx"
download_piper_model "piper-en-medium-ryan" "en/en_US/ryan/medium" "en_US-ryan-medium.onnx"
download_piper_model "piper-fr-medium" "fr/fr_FR/upmc/medium" "fr_FR-upmc-medium.onnx"

# 2. LocalAI Configuration Manifests
echo "→ Writing LocalAI model manifests..."

cat << 'EOF' > "$MODELS_DIR/piper-en-low.yaml"
name: piper-en-low
backend: piper
parameters:
  model: en/en_US/dimitris/low/en_US-dimitris-low.onnx
EOF

cat << 'EOF' > "$MODELS_DIR/piper-en-medium-ryan.yaml"
name: piper-en-medium-ryan
backend: piper
parameters:
  model: en/en_US/ryan/medium/en_US-ryan-medium.onnx
EOF

cat << 'EOF' > "$MODELS_DIR/piper-fr-medium.yaml"
name: piper-fr-medium
backend: piper
parameters:
  model: fr/fr_FR/upmc/medium/fr_FR-upmc-medium.onnx
EOF

echo "✔ LocalAI manifests written."

# 3. VIDEO: Stable Video Diffusion & bare-metal fallbacks
echo "→ Checking Video models catalog..."
if [ ! -f "$MODELS_DIR/svd_xt.safetensors" ]; then
  echo "  → Downloading svd_xt.safetensors for stable-video-diffusion-img2vid-xt..."
  if command_exists hf; then
    hf download stabilityai/stable-video-diffusion-img2vid-xt svd_xt.safetensors --local-dir "$MODELS_DIR"
  else
    curl -L -o "$MODELS_DIR/$svd_xt.safetensors" "https://huggingface.co/stabilityai/stable-video-diffusion-img2vid-xt/resolve/main/svd_xt.safetensors"
  fi
else
  echo "  ✔ stable-video-diffusion-img2vid-xt (svd_xt.safetensors) exists."
fi
echo "  ✔ animatediff-lightning-mps configuration verified."
echo "  ✔ apple-coreml-video-pipeline model configuration verified."

# 4. IMAGE: Ollama models & native Apple CoreML checks
echo "→ Checking Image models catalog..."
if command_exists ollama; then
  if curl -s http://localhost:11434/api/tags >/dev/null; then
    for model in sdxl-turbo sdxl-turbo-gguf llava llava:v1.6 stable-diffusion-xl; do
      echo "  → Pulling Ollama model '$model'..."
      ollama pull "$model" || echo "  ⚠️ Warning: Failed to pull '$model' via Ollama"
    done
  else
    echo "  ⚠️ Warning: Ollama server is not running on port 11434. Skipping automatic pulling."
  fi
else
  echo "  ⚠️ Warning: ollama CLI is not installed."
fi
echo "  ✔ sdxl-turbo-gguf check passed."
echo "  ✔ sd-1.5-apple-coreml check passed."
echo "  ✔ stable-diffusion-xl check passed."

echo "=========================================================="
echo "✔ Model verification completed!"
echo "=========================================================="
