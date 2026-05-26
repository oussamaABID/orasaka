# Tested AI Models Catalog

> Specifications of the pre-seeded and tested models in the Orasaka database catalog.

---

## 1. Speech (Text-to-Speech)

Speech synthesis models convert text prompts into audio files (returns `202 Accepted` job ID).

| Model Name | Model Label | Category | Default Voices | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `piper-en-low` | Piper Low (en) | Speech | `Ryan`, `Low` | [Hugging Face Voices](https://huggingface.co/rhasspy/piper-voices) | [OHF-Voice Piper GitHub](https://github.com/OHF-Voice/piper1-gpl) |
| `piper-en-medium-ryan` | Piper Ryan (en) | Speech | `Ryan`, `Medium` | [Hugging Face Voices](https://huggingface.co/rhasspy/piper-voices) | [OHF-Voice Piper GitHub](https://github.com/OHF-Voice/piper1-gpl) |
| `piper-fr-medium` | Piper Medium (fr) | Speech | `Medium`, `Fr` | [Hugging Face Voices](https://huggingface.co/rhasspy/piper-voices) | [OHF-Voice Piper GitHub](https://github.com/OHF-Voice/piper1-gpl) |
| `tts-1` | OpenAI TTS-1 | Speech | `Alloy`, `Echo`, `Fable`, `Onyx`, `Nova`, `Shimmer` | [OpenAI API](https://platform.openai.com) | [OpenAI TTS Documentation](https://platform.openai.com/docs/guides/text-to-speech) |

- **REST Endpoint**: `POST /api/v1/chat/speech`
- **Request Payload**:
  ```json
  {
    "model": "piper-en-medium-ryan",
    "text": "Hello, this is a local speech synthesis test.",
    "voice": "Ryan"
  }
  ```
- **CLI Command**:
  ```bash
  npx orasaka chat --speech "Hello from Orasaka" --save output.wav
  ```

---

## 2. Image (Text-to-Image)

Image models generate static graphics from textual descriptions.

| Model Name | Model Label | Category | Purpose | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `sdxl-turbo-gguf` | SDXL Turbo (GGUF) | Image | Fast execution on CPU/Metal | [Hugging Face Repository](https://huggingface.co/stabilityai/sdxl-turbo) | [stable-diffusion.cpp GitHub](https://github.com/leejet/stable-diffusion.cpp) |
| `sd-1.5-apple-coreml` | SD 1.5 (Apple CoreML) | Image | Apple Silicon optimized CoreML | [Hugging Face CoreML Models](https://huggingface.co/apple/coreml-stable-diffusion-v1-5) | [Apple ML-Stable-Diffusion Repo](https://github.com/apple/ml-stable-diffusion) |
| `stable-diffusion-xl` | Stable Diffusion XL | Image | High-fidelity SDXL renders (default) | [Hugging Face SDXL Base](https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0) | [Stability AI Generative Models](https://github.com/Stability-AI/generative-models) |
| `v1-5-pruned-emaonly` | SD 1.5 Pruned EMA (safetensors) | Image | Apple Silicon MLX optimized | [Hugging Face RunwayML](https://huggingface.co/runwayml/stable-diffusion-v1-5) | [MLX Examples GitHub](https://github.com/ml-explore/mlx-examples) |

- **REST Endpoint**: `POST /api/v1/chat/image`
- **Request Payload**:
  ```json
  {
    "prompt": "A beautiful neon cyberpunk skyline",
    "model": "stable-diffusion-xl"
  }
  ```
- **CLI Command**:
  ```bash
  npx orasaka chat --gen-image "A beautiful neon cyberpunk skyline" --save skyline.png
  ```

---

## 3. Video (Text-to-Video)

Asynchronous video rendering workflows processed via Python workers and RabbitMQ.

| Model Name | Model Label | Category | Platform Engine | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `stable-video-diffusion-img2vid-xt` | Stable Video Diffusion XT | Video | SVD img2vid on Metal/MPS (default) | [Hugging Face SVD-XT](https://huggingface.co/stabilityai/stable-video-diffusion-img2vid-xt) | [Diffusers SVD Documentation](https://huggingface.co/docs/diffusers/using-diffusers/svd) |
| `animatediff-lightning-mps` | AnimateDiff Lightning (MPS) | Video | Fast AnimateDiff rendering | [Hugging Face AnimateDiff-Lightning](https://huggingface.co/ByteDance/AnimateDiff-Lightning) | [AnimateDiff Original GitHub](https://github.com/guoyww/AnimateDiff) |
| `apple-coreml-video-pipeline` | Apple CoreML Video Pipeline | Video | MPS CoreML native pipeline | [Hugging Face CoreML Models](https://huggingface.co/apple/coreml-stable-diffusion-v1-5) | [Apple ML-Stable-Diffusion Repo](https://github.com/apple/ml-stable-diffusion) |
| `stable-video-diffusion-img2vid-xt-mps-fp32` | SVD XT (PyTorch MPS Float32) | Video | Apple Silicon MLX float32 | [Hugging Face SVD-XT](https://huggingface.co/stabilityai/stable-video-diffusion-img2vid-xt) | [MLX Examples GitHub](https://github.com/ml-explore/mlx-examples) |

- **REST Endpoint**: `POST /api/v1/ai/video`
- **Request Payload**:
  ```json
  {
    "prompt": "Cinematic shot of cyberpunk streets, neon lights, heavy rain",
    "image": "550e8400-e29b-41d4-a716-446655440002",
    "model": "stable-video-diffusion-img2vid-xt",
    "durationSeconds": 4
  }
  ```
- **CLI Command**:
  ```bash
  npx orasaka video "A cinematic shot of cyberpunk streets" --save output.mp4
  ```

---

## 4. Vision (Multimodal Ingestion)

Multimodal analysis models processing images via Ollama.

| Model Name | Model Label | Category | Platform | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `llama3.2-vision:latest` | Llama 3.2 Vision (latest) | Vision | Ollama host vision (default) | [Ollama Model Page](https://ollama.com/library/llama3.2-vision) | [Meta Llama 3.2 Hugging Face](https://huggingface.co/meta-llama/Llama-3.2-11B-Vision-Instruct) |
| `llava:latest` | LLaVA (latest) | Vision | Multimodal fallback | [Ollama Model Page](https://ollama.com/library/llava) | [LLaVA Original GitHub](https://github.com/haotian-liu/LLaVA) |
| `llava:v1.6` | LLaVA (v1.6) | Vision | LLaVA version 1.6 | [Ollama Model Page](https://ollama.com/library/llava) | [LLaVA Original GitHub](https://github.com/haotian-liu/LLaVA) |
| `bakllava:latest` | BakLLaVA (latest) | Vision | BakLLaVA multimodal | [Ollama Model Page](https://ollama.com/library/bakllava) | [BakLLaVA Original GitHub](https://github.com/SkunkworksAI/BakLLaVA) |

- **REST Endpoint**: `POST /api/v1/media/analyze-image`
- **Request Payload**:
  ```json
  {
    "model": "llama3.2-vision:latest",
    "prompt": "Identify elements in this poster",
    "assetId": "550e8400-e29b-41d4-a716-446655440002"
  }
  ```
- **CLI Command**:
  ```bash
  npx orasaka chat --image "/path/to/poster.jpg" "Describe this design"
  ```

---

## 5. Audio (Speech-to-Text)

Speech transcription using local Whisper engines via Ollama.

| Model Name | Model Label | Category | Platform | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `whisper-base` | Whisper Base | Audio | Ollama (default) | [Ollama Model Page](https://ollama.com/library/whisper) | [OpenAI Whisper GitHub](https://github.com/openai/whisper) |
| `whisper-tiny-en` | Whisper Tiny (en) | Audio | Ollama lightweight | [Ollama Model Page](https://ollama.com/library/whisper) | [OpenAI Whisper GitHub](https://github.com/openai/whisper) |

- **REST Endpoint**: `POST /api/v1/media/analyze-audio`
- **Request Payload**:
  ```json
  {
    "assetId": "550e8400-e29b-41d4-a716-446655440003",
    "threadId": "550e8400-e29b-41d4-a716-446655440004"
  }
  ```
- **CLI Command**:
  ```bash
  npx orasaka chat --audio "/path/to/recording.wav"
  ```

---

## 6. Code (Code Generation)

Code-specialized LLMs for assisted generation and refactoring.

| Model Name | Model Label | Category | Quantization | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `qwen2.5-coder:7b` | Qwen 2.5 Coder 7B | Code | `q4_K_M` (default) | [Ollama Model Page](https://ollama.com/library/qwen2.5-coder) | [Qwen2.5-Coder GitHub](https://github.com/QwenLM/Qwen2.5-Coder) |
| `codellama:7b` | CodeLlama 7B | Code | `q4_K_M` | [Ollama Model Page](https://ollama.com/library/codellama) | [Code Llama Meta Docs](https://huggingface.co/codellama/CodeLlama-7b-Instruct-hf) |

---

## 7. MLX Native Models (Apple Silicon Unified Memory)

Models optimized for the Apple Silicon [MLX Framework](https://github.com/ml-explore/mlx) (unified memory architecture).

| Model Name | Model Label | Category | Hardware | Model Hub / Download | Documentation & Repo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `argmaxinc/mlx-FLUX.1-schnell-4bit-quantized` | FLUX.1 Schnell 4-bit (MLX) | Image | Apple Silicon MLX | [Hugging Face Argmax MLX FLUX](https://huggingface.co/argmaxinc/mlx-FLUX.1-schnell-4bit-quantized) | [Argmax Swift-FLUX GitHub](https://github.com/argmaxinc/swift-flux) |
| `ByteDance/AnimateDiff-Lightning` | AnimateDiff Lightning | Video | Apple Silicon MLX | [Hugging Face AnimateDiff-Lightning](https://huggingface.co/ByteDance/AnimateDiff-Lightning) | [ByteDance Repo](https://huggingface.co/ByteDance/AnimateDiff-Lightning) |
| `mlx-animatediff-lightning` | MLX Native AnimateDiff | Video | Apple Silicon MLX | [Hugging Face MLX Repository](https://huggingface.co) | [MLX Examples GitHub](https://github.com/ml-explore/mlx-examples) |
| `mlx-stable-diffusion-video` | MLX Native Video | Video | Apple Silicon MLX | [Hugging Face MLX Repository](https://huggingface.co) | [MLX Examples GitHub](https://github.com/ml-explore/mlx-examples) |

---

## 8. Verified Local Execution Outputs

Real outputs captured during model runs on macOS Apple Silicon M1 node:

- **SD 1.5 Image**: [Generation Log Config](file:///Users/oussamaabid/Documents/projects/orasaka/docs/assets/orasaka/output/image/sd-1.5/stable-diffusion-cpp/prompt.md)
  ![Verified Image](file:///Users/oussamaabid/Documents/projects/orasaka/docs/assets/orasaka/output/image/sd-1.5/stable-diffusion-cpp/image_output_20260601_204836.png)

- **AnimateDiff Video**: [Video Pipeline Prompt Logs](file:///Users/oussamaabid/Documents/projects/orasaka/docs/assets/orasaka/output/video/animatediff-lightning/diffusers-pytorch/prompt.md)
  <video src="https://github.com/user-attachments/assets/4a643384-358b-4b6d-b02f-1a4c037bbc0b" autoplay loop muted playsinline controls width="100%"></video>

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Architecture Reference](ARCHITECTURE.md)
- [API Reference](API_REFERENCE.md)
- [Core Deep-Dive](CORE.md)
