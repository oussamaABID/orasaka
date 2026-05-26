#!/usr/bin/env bash
# ops/local/install-tooling.sh
# Automates the installation of Terraform, AWS CLI, and serverless CLI tools on macOS.

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=========================================================="
echo "🥷 Orasaka Production Tooling Installation Script"
echo "=========================================================="

# Check for macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
  echo "Error: This script is configured for macOS (Darwin)." >&2
  exit 1
fi

# Function to check command existence
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Check or install Homebrew
if ! command_exists brew; then
  echo "→ Homebrew not found. Installing Homebrew..."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  # Add to shell profile dynamically for current session
  eval "$(/opt/homebrew/bin/brew shellenv || /usr/local/bin/brew shellenv)"
else
  echo "✔ Homebrew is already installed."
fi

# 1. Install Terraform CLI
if ! command_exists terraform; then
  echo "→ Installing HashiCorp Terraform CLI..."
  brew tap hashicorp/tap
  brew install hashicorp/tap/terraform
  echo "✔ Terraform successfully installed: $(terraform -version | head -n 1)"
else
  echo "✔ Terraform is already installed: $(terraform -version | head -n 1)"
fi

# 2. Install AWS CLI
if ! command_exists aws; then
  echo "→ Installing AWS CLI..."
  brew install awscli
  echo "✔ AWS CLI successfully installed: $(aws --version)"
else
  echo "✔ AWS CLI is already installed: $(aws --version)"
fi

# 2b. Install FFmpeg
if ! command_exists ffmpeg; then
  echo "→ Installing FFmpeg CLI for local audio extraction..."
  brew install ffmpeg
  echo "✔ FFmpeg successfully installed: $(ffmpeg -version | head -n 1)"
else
  echo "✔ FFmpeg is already installed: $(ffmpeg -version | head -n 1)"
fi

# 3. Install Python & Modal CLI
if ! command_exists python3; then
  echo "→ Installing Python 3..."
  brew install python
fi

if ! command_exists modal; then
  echo "→ Installing Modal CLI via pip..."
  pip3 install modal --break-system-packages
  echo "✔ Modal CLI successfully installed."
else
  echo "✔ Modal CLI is already installed."
fi

echo "→ Checking and installing Python dependencies for stable diffusion and video inference..."
pip3 install torch torchvision diffusers accelerate safetensors transformers clean-fid --break-system-packages || {
  echo "→ Standard pip install failed. Trying without break-system-packages..."
  pip3 install torch torchvision diffusers accelerate safetensors transformers clean-fid
}

# 4. Install LocalAI
echo "→ Checking LocalAI installation..."
LOCALAI_BIN=""
if command_exists local-ai; then
  LOCALAI_BIN="local-ai"
elif command_exists localai; then
  LOCALAI_BIN="localai"
elif [ -f "$SCRIPT_DIR/../bin/local-ai" ]; then
  LOCALAI_BIN="$SCRIPT_DIR/../bin/local-ai"
elif [ -f "$SCRIPT_DIR/../bin/localai" ]; then
  LOCALAI_BIN="$SCRIPT_DIR/../bin/localai"
fi

if [ -z "$LOCALAI_BIN" ]; then
  echo "→ LocalAI not found. Attempting install via Homebrew..."
  if brew install localai; then
    echo "✔ LocalAI successfully installed via Homebrew."
    LOCALAI_BIN="localai"
  else
    echo "→ Homebrew install failed or skipped. Downloading native static binary for macOS Apple Silicon..."
    mkdir -p "$SCRIPT_DIR/../bin"
    curl -L -o "$SCRIPT_DIR/../bin/local-ai" "https://github.com/mudler/LocalAI/releases/download/v4.3.1/local-ai-v4.3.1-darwin-arm64"
    chmod +x "$SCRIPT_DIR/../bin/local-ai"
    xattr -d com.apple.quarantine "$SCRIPT_DIR/../bin/local-ai" 2>/dev/null || true
    echo "✔ LocalAI static binary downloaded and prepared at ops/local/bin/local-ai"
    LOCALAI_BIN="$SCRIPT_DIR/../bin/local-ai"
  fi
else
  echo "✔ LocalAI is already installed at: $LOCALAI_BIN"
fi

# 5. Provision models via huggingface-cli
echo "→ Provisioning public model weights via huggingface-cli..."
MODELS_DIR="$HOME/models/stable-diffusion"
mkdir -p "$MODELS_DIR"

if command_exists huggingface-cli; then
  # A) Stable Video Diffusion checkpoint
  if [ ! -f "$MODELS_DIR/svd_xt.safetensors" ]; then
    echo "→ Pulling 'svd_xt.safetensors' from HF stabilityai/stable-video-diffusion-img2vid-xt..."
    huggingface-cli download stabilityai/stable-video-diffusion-img2vid-xt svd_xt.safetensors --local-dir "$MODELS_DIR" --local-dir-use-symlinks False
  else
    echo "✔ 'svd_xt.safetensors' already exists."
  fi

  # B) Whisper STT model
  if [ ! -f "$MODELS_DIR/ggml-tiny.bin" ]; then
    echo "→ Pulling 'ggml-tiny.bin' from HF ggerganov/whisper.cpp..."
    huggingface-cli download ggerganov/whisper.cpp ggml-tiny.bin --local-dir "$MODELS_DIR" --local-dir-use-symlinks False
  else
    echo "✔ 'ggml-tiny.bin' already exists."
  fi

  # C) Piper TTS model and JSON config
  if [ ! -f "$MODELS_DIR/en/en_US/lessac/medium/en_US-lessac-medium.onnx" ]; then
    echo "→ Pulling Piper TTS ONNX model from HF rhasspy/piper-voices..."
    huggingface-cli download rhasspy/piper-voices en/en_US/lessac/medium/en_US-lessac-medium.onnx --local-dir "$MODELS_DIR" --local-dir-use-symlinks False
  fi
  if [ ! -f "$MODELS_DIR/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json" ]; then
    echo "→ Pulling Piper TTS JSON config from HF rhasspy/piper-voices..."
    huggingface-cli download rhasspy/piper-voices en/en_US/lessac/medium/en_US-lessac-medium.onnx.json --local-dir "$MODELS_DIR" --local-dir-use-symlinks False
  fi
else
  echo "❌ Error: huggingface-cli is missing. Cannot automatically pull models." >&2
  exit 1
fi

# 6. Write LocalAI YAML configurations
echo "→ Setting up LocalAI YAML model definitions..."
cat << 'EOF' > "$MODELS_DIR/whisper-1.yaml"
name: whisper-1
backend: whisper
parameters:
  model: ggml-tiny.bin
EOF

cat << 'EOF' > "$MODELS_DIR/tts-1.yaml"
name: tts-1
backend: piper
parameters:
  model: en/en_US/lessac/medium/en_US-lessac-medium.onnx
EOF

echo "✔ LocalAI model configs written successfully."

echo "=========================================================="
echo "✔ Tooling setup completed successfully!"
echo "Please restart your terminal or source your profile to finalize paths."
echo "=========================================================="
