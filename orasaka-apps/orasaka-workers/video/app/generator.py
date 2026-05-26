import os
import sys
import gc
from PIL import Image, ImageDraw

try:
    import psutil
except ImportError:
    psutil = None

try:
    import torch
except ImportError:
    torch = None

try:
    from diffusers import StableVideoDiffusionPipeline
except ImportError:
    StableVideoDiffusionPipeline = None

# Global pipeline instance — LAZY loaded to avoid 13GB MPS allocation
# when AnimateDiff-Lightning is the active pipeline.
pipe = None
_svd_initialized = False


def init_pipeline():
    """Lazy-load SVD pipeline on first request (not at module import)."""
    global pipe, _svd_initialized
    if _svd_initialized:
        return
    _svd_initialized = True

    if StableVideoDiffusionPipeline is None or torch is None:
        print("SVD diffusers or torch not installed. SVD pipeline will not be available.", flush=True)
        return

    print("Loading Stable Video Diffusion pipeline...", flush=True)
    model_path = "stabilityai/stable-video-diffusion-img2vid-xt"

    is_mps = torch.backends.mps.is_available()
    dtype = torch.float32 if is_mps else torch.float16
    variant = "fp16"

    try:
        try:
            pipe = StableVideoDiffusionPipeline.from_pretrained(
                model_path,
                torch_dtype=dtype,
                variant=variant,
                local_files_only=True
            )
        except Exception as cache_err:
            print(f"Could not load from local cached files: {cache_err}. Attempting online download...", flush=True)
            pipe = StableVideoDiffusionPipeline.from_pretrained(
                model_path,
                torch_dtype=dtype,
                variant=variant
            )

        # Configure host target device
        if is_mps:
            pipe.to("mps")
            print("Model loaded successfully on Apple Silicon GPU (MPS).", flush=True)
        elif torch.cuda.is_available():
            pipe.to("cuda")
            print("Model loaded successfully on CUDA GPU.", flush=True)
        else:
            pipe.to("cpu")
            print("Model loaded on CPU.", flush=True)

        pipe.enable_attention_slicing()
        pipe.unet.set_default_attn_processor()
        if hasattr(pipe, "vae"):
            pipe.vae.set_default_attn_processor()
    except Exception as e:
        print(f"Failed to load SVD model: {e}", flush=True)


# NOTE: SVD is NOT loaded at module import. Call init_pipeline() before use.
print("SVD pipeline: DEFERRED (lazy-load on first SVD request).", flush=True)

def generate_fallback_frames(input_image: Image.Image, num_frames: int = 14):
    frames = []
    w, h = input_image.size
    for i in range(num_frames):
        # Prevent division by zero if num_frames = 1
        denom = max(1, num_frames - 1)
        zoom = 1.0 + (i / denom) * 0.15
        dx = int((i / denom) * 30)
        dy = int((i / denom) * 20)
        
        new_w, new_h = int(w * zoom), int(h * zoom)
        zoomed = input_image.resize((new_w, new_h), Image.Resampling.LANCZOS)
        
        left = (new_w - w) // 2 + dx
        top = (new_h - h) // 2 + dy
        left = max(0, min(left, new_w - w))
        top = max(0, min(top, new_h - h))
        
        frame = zoomed.crop((left, top, left + w, top + h)).convert("RGBA")
        
        overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        draw = ImageDraw.Draw(overlay)
        
        sweep_x = int((i / denom) * (w + 200)) - 100
        
        for offset in range(-40, 40):
            alpha = int((1.0 - abs(offset) / 40) * 80)
            draw.line([(sweep_x + offset - 100, 0), (sweep_x + offset + 100, h)], fill=(0, 255, 255, alpha), width=1)
            
        composed = Image.alpha_composite(frame, overlay).convert("RGB")
        frames.append(composed)
    return frames

def generate_video_frames(
    input_image: Image.Image,
    num_frames: int,
    num_inference_steps: int,
    generator,
    progress_callback
) -> list:
    """Runs video generation using SVD with dynamic memory checks.

    Falls back to generate_fallback_frames on any OOM or inference failure
    to guarantee a valid video output is always returned.
    """
    process = psutil.Process(os.getpid()) if psutil else None

    # Lazy-load SVD pipeline on first request
    init_pipeline()

    if pipe is None:
        raise RuntimeError("SVD pipeline is not initialized. Cannot generate video without model.")

    if psutil and process:
        try:
            process_mem_mb = process.memory_info().rss / (1024 * 1024)
            sys_mem = psutil.virtual_memory()
            available_mem_mb = sys_mem.available / (1024 * 1024)
            print(f"Memory Check: Process RSS = {process_mem_mb:.2f} MB, System Available = {available_mem_mb:.2f} MB", flush=True)
            
            # Dynamic memory slicing/tiling thresholds
            MEM_SLICING_THRESHOLD_MB = 4000.0
            MEM_AVAIL_WARNING_MB = 2000.0
            SYSTEM_AVAIL_MIN_MB = 1500.0
            
            if available_mem_mb < SYSTEM_AVAIL_MIN_MB:
                raise RuntimeError(f"System memory exhausted: {available_mem_mb:.0f} MB available (min: {SYSTEM_AVAIL_MIN_MB} MB)")
                
            if process_mem_mb > MEM_SLICING_THRESHOLD_MB or available_mem_mb < MEM_AVAIL_WARNING_MB:
                print("Memory threshold reached. Enabling dynamic VAE slicing, tiling, and attention slicing to prevent OOM.", flush=True)
                if hasattr(pipe, "enable_vae_slicing"):
                    pipe.enable_vae_slicing()
                if hasattr(pipe, "enable_vae_tiling"):
                    pipe.enable_vae_tiling()
                if hasattr(pipe, "enable_attention_slicing"):
                    pipe.enable_attention_slicing()
        except MemoryError:
            raise RuntimeError("MemoryError during pre-check. System cannot allocate for video generation.")
        except Exception as mem_err:
            print(f"Error checking memory: {mem_err}", flush=True)
            
    import traceback
    try:
        print(f"Executing SVD pipeline for {num_frames} frames...", flush=True)
        decode_chunk_size = 4 if num_frames >= 4 else 1
        # SVD pipeline call
        svd_frames = pipe(
            input_image, 
            decode_chunk_size=decode_chunk_size, 
            num_frames=num_frames, 
            num_inference_steps=num_inference_steps, 
            generator=generator,
            callback_on_step_end=progress_callback
        ).frames[0]
        
        if not svd_frames or len(svd_frames) == 0:
            raise ValueError("SVD output frames are empty")
        return svd_frames
    except Exception as svd_err:
        print(f"SVD execution failed: {svd_err}. FAILING LOUDLY — no fallback frames.", flush=True)
        traceback.print_exc()
        # Force garbage collection to free any partially-allocated tensors
        gc.collect()
        if torch is not None and hasattr(torch, 'mps') and torch.backends.mps.is_available():
            torch.mps.empty_cache()
        elif torch is not None and torch.cuda.is_available():
            torch.cuda.empty_cache()
        # §12 Zero-Fallback Policy: re-raise instead of masking with Pillow gradients
        raise RuntimeError(f"SVD inference failed (no fallback): {svd_err}") from svd_err

