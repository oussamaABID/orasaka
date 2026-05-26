#!/usr/bin/env python3
import os
import sys
import json
import base64
import tempfile
import io
from http.server import BaseHTTPRequestHandler, HTTPServer
from PIL import Image
from typing import Any, Dict

try:
    import psutil
except ImportError:
    psutil = None

try:
    import torch
except ImportError:
    torch = None

from app.telemetry import send_progress_update, resource_guard
from app.encoder import export_frames_to_video
from app.generator import generate_video_frames, generate_fallback_frames

try:
    from app.mlx_generator import generate_video_frames_mlx
except ImportError:
    generate_video_frames_mlx = None

MODEL_REGISTRY: Dict[str, str] = {
    "stable-video-diffusion-img2vid-xt": "SVD",
    "animatediff-lightning-mps": "AnimateDiff",
    "apple-coreml-video-pipeline": "CoreML",
    "mlx-animatediff-lightning": "MLX-AnimateDiff",
    "mlx-stable-diffusion-video": "MLX-SVD",
    "stable-video-diffusion-img2vid-xt-mps-fp32": "SVD-MPS-FP32",
    # MLX Community HuggingFace models
    "argmaxinc/mlx-FLUX.1-schnell-4bit-quantized": "MLX-FLUX-Schnell",
    "ByteDance/AnimateDiff-Lightning": "MLX-AnimateDiff-Lightning",
}

def load_image(payload: Dict[str, Any]) -> Image.Image:
    """Ingests starting image from base64 data or a local path, falling back to a default."""
    img = Image.new("RGB", (1024, 576), (128, 128, 128))
    if payload.get("image_path"):
        try:
            path = payload["image_path"]
            if os.path.exists(path):
                return Image.open(path).convert("RGB").resize((1024, 576))
        except Exception as e:
            print(f"Could not open image_path: {e}", flush=True)
    elif payload.get("image"):
        try:
            img_data = base64.b64decode(payload["image"])
            return Image.open(io.BytesIO(img_data)).convert("RGB").resize((1024, 576))
        except Exception as e:
            print(f"Could not parse base64 image: {e}", flush=True)
    return img


def _generate_flux_image(
    prompt: str,
    model_name: str,
    steps: int,
    num_frames: int,
    seed: int,
) -> list:
    """Generate image frames using FLUX.1 via mflux (MLX native).

    Falls back to a prompt-colored gradient image if mflux is not installed.
    Returns a list of PIL Images suitable for video encoding.
    """
    try:
        from mflux import Flux1, Config

        print(f"Initializing FLUX via mflux: model={model_name}, steps={steps}, seed={seed}", flush=True)
        flux = Flux1(
            model_alias="schnell",
            quantize=4,
        )
        image = flux.generate_image(
            seed=seed,
            prompt=prompt,
            config=Config(
                num_inference_steps=steps,
                height=576,
                width=1024,
            ),
        )
        pil_image = image.image
        print(f"FLUX inference completed: {pil_image.size}", flush=True)
        # Return duplicated frames for video encoding compatibility
        return [pil_image.copy() for _ in range(num_frames)]

    except ImportError:
        raise RuntimeError("mflux is not installed. Cannot generate FLUX image without the dependency.")
    except Exception as e:
        raise RuntimeError(f"FLUX inference failed: {e}") from e


class VideoInferenceHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        if self.path == '/':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"status": "running"}).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self) -> None:
        if self.path != '/v1/videos/generations':
            self.send_response(404)
            self.end_headers()
            return

        content_length = int(self.headers.get('Content-Length', 0))
        try:
            payload: Dict[str, Any] = json.loads(self.rfile.read(content_length).decode('utf-8'))
        except Exception:
            payload = {}

        model_name = payload.get("model", "stable-video-diffusion-img2vid-xt")
        if model_name not in MODEL_REGISTRY:
            self.send_response(400)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"error": f"Model '{model_name}' is not registered."}).encode('utf-8'))
            return

        process = psutil.Process(os.getpid()) if psutil else None
        input_image = load_image(payload)
        try:
            with resource_guard(process) as metrics:
                seed_str = os.environ.get("ORASAKA_VIDEO_GEN_SEED", "-1")
                try:
                    seed_val = int(seed_str)
                except ValueError:
                    seed_val = -1

                if seed_val == -1:
                    import random
                    seed_val = random.randint(0, 2147483647)

                print(f"Using video generation seed: {seed_val}", flush=True)
                generator = torch.manual_seed(seed_val) if torch else None
                job_id = payload.get("job_id")
                
                model_name = payload.get("model", "stable-video-diffusion-img2vid-xt")
                steps = payload.get("videoSteps") or payload.get("video_steps")
                fps_render = payload.get("videoFps") or payload.get("video_fps")

                if not steps:
                    model_lower = model_name.lower()
                    if "flux" in model_lower:
                        steps = 4  # FLUX Schnell distilled
                    elif "animatediff" in model_lower:
                        steps = 16  # Deep inference for 64GB Unified Memory
                    else:
                        steps = 25

                if not fps_render:
                    model_lower = model_name.lower()
                    if "animatediff" in model_lower:
                        fps_render = 12
                    elif "flux" in model_lower:
                        fps_render = 1  # FLUX generates still images
                    else:
                        fps_render = 14

                if job_id:
                    send_progress_update(job_id, 0)
                
                def progress_cb(pipe_self: Any, step: int, timestep: Any, kwargs: Dict[str, Any]) -> Dict[str, Any]:
                    if job_id:
                        progress = min(int((step + 1) / steps * 100), 100)
                        send_progress_update(job_id, progress)
                    return kwargs

                duration = payload.get("durationSeconds") or payload.get("video_length") or 2
                # Resolution from payload (defaults handled by mlx_generator)
                req_width = payload.get("width")
                req_height = payload.get("height")

                # MLX models: minimum 16 frames for cinematic output (4.0s at 4fps)
                is_mlx_model = model_name.startswith("mlx-") or model_name.startswith("mlx-community/") or model_name == "ByteDance/AnimateDiff-Lightning"
                min_frames = 16 if is_mlx_model else 14
                frames_n = max(min_frames, min(int(duration * fps_render), 30))

                # Allow explicit num_frames override from payload (backward-compatible control pathway)
                explicit_frames = payload.get("num_frames")
                if explicit_frames and int(explicit_frames) > 0:
                    frames_n = int(explicit_frames)

                if model_name.startswith("argmaxinc/"):
                    # FLUX image generation via mflux (MLX native)
                    frames = _generate_flux_image(
                        prompt=payload.get("prompt", ""),
                        model_name=model_name,
                        steps=steps,
                        num_frames=frames_n,
                        seed=seed_val,
                    )
                elif is_mlx_model:
                    if generate_video_frames_mlx is None:
                        raise ImportError("AnimateDiff-Lightning pipeline is not available on this host.")
                    mlx_kwargs = {
                        "input_image": input_image,
                        "num_frames": frames_n,
                        "num_inference_steps": steps,
                        "progress_callback": progress_cb,
                        "prompt": payload.get("prompt", ""),
                    }
                    if req_width:
                        mlx_kwargs["width"] = int(req_width)
                    if req_height:
                        mlx_kwargs["height"] = int(req_height)
                    frames = generate_video_frames_mlx(**mlx_kwargs)
                else:
                    frames = generate_video_frames(
                        input_image=input_image,
                        num_frames=frames_n,
                        num_inference_steps=steps,
                        generator=generator,
                        progress_callback=progress_cb
                    )

                output_path = payload.get("output_path")
                if output_path:
                    os.makedirs(os.path.dirname(output_path), exist_ok=True)
                    export_frames_to_video(frames, output_path, fps=fps_render)
                    if job_id:
                        send_progress_update(job_id, 100)
                    response_data = {"status": "success", "output_path": output_path, "metrics": metrics}
                else:
                    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
                        tmp_path = tmp.name
                    try:
                        export_frames_to_video(frames, tmp_path, fps=fps_render)
                        with open(tmp_path, "rb") as f:
                            video_bytes = f.read()
                    finally:
                        if os.path.exists(tmp_path):
                            os.remove(tmp_path)
                    
                    base64_video = base64.b64encode(video_bytes).decode('utf-8')
                    if job_id:
                        send_progress_update(job_id, 100)
                    response_data = {"data": [{"b64_json": base64_video}], "metrics": metrics}

            body = json.dumps(response_data)
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body.encode('utf-8'))
        except Exception as e:
            print(f"Error during video generation: {e}", flush=True)
            self.send_response(500)
            self.end_headers()
            self.wfile.write(str(e).encode('utf-8'))

def run(port: int = 8188) -> None:
    server_address = ('', port)
    httpd = HTTPServer(server_address, VideoInferenceHandler)
    print(f"Video Worker listening on port {port}...", flush=True)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        httpd.server_close()

if __name__ == '__main__':
    port_arg = 8188
    if len(sys.argv) > 1:
        port_arg = int(sys.argv[1])
    run(port_arg)
