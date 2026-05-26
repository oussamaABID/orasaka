import os
import json
import time
import gc
import pika
from contextlib import contextmanager

try:
    import torch
except ImportError:
    torch = None

def send_progress_update(job_id: str, progress: int) -> None:
    host = os.environ.get("SPRING_RABBITMQ_HOST", "localhost")
    try:
        port = int(os.environ.get("SPRING_RABBITMQ_PORT", "5672"))
    except ValueError:
        port = 5672
        
    try:
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=host, port=port)
        )
        channel = connection.channel()
        channel.exchange_declare(exchange='orasaka.jobs.exchange', exchange_type='direct', durable=True)
        
        payload = {
            "jobId": job_id,
            "progress": int(progress)
        }
        message = json.dumps(payload)
        
        channel.basic_publish(
            exchange='orasaka.jobs.exchange',
            routing_key='orasaka.progress.routingKey',
            body=message,
            properties=pika.BasicProperties(
                content_type='application/json',
                delivery_mode=2
            )
        )
        connection.close()
        print(f"Sent AMQP progress update to exchange: {message}", flush=True)
    except Exception as e:
        print(f"Failed to send AMQP progress update to RabbitMQ: {e}", flush=True)

@contextmanager
def resource_guard(process):
    """
    Context manager that tracks execution duration, peak RSS memory usage,
    and ensures garbage collection and GPU cache eviction are run.
    """
    start_time = time.time()
    metrics = {}
    try:
        yield metrics
    finally:
        gc.collect()
        if torch and hasattr(torch, "backends") and hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
            try:
                torch.mps.empty_cache()
            except Exception:
                pass
            
        end_time = time.time()
        end_mem = process.memory_info().rss if process else 0
        gpu_allocated_mb = 0.0
        if torch and torch.cuda.is_available():
            gpu_allocated_mb = torch.cuda.max_memory_allocated() / (1024 * 1024)
            
        metrics["inference_time_sec"] = round(end_time - start_time, 2)
        metrics["peak_memory_rss_mb"] = round(end_mem / (1024 * 1024), 2)
        metrics["gpu_allocated_mb"] = round(gpu_allocated_mb, 2)
