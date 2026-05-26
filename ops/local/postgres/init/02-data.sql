-- ============================================================================
-- 1. IDENTITY SEED DATA
-- ============================================================================

INSERT INTO orasaka_rate_limit_tiers (id, capacity, refill_tokens, refill_seconds) VALUES
('free', 5, 5, 60),
('premium', 50, 50, 60),
('admin', 1000, 1000, 60)
ON CONFLICT (id) DO NOTHING;

INSERT INTO orasaka_users (id, username, password_hash, email, enabled, preferences, rate_limit_tier) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'admin', '$2a$10$SDoTA/b9FdJl4Aw2hQBmAufpudMESJwmPpHPPxZUsqxMo68JYGJYu', 'admin@orasaka.com', true, '{"language":"en", "tts-voice":"alloy", "image-aspect-ratio":"16:9", "chat-temperature":0.7}', 'admin'),
('550e8400-e29b-41d4-a716-446655440002', 'user', '$2a$10$gk.zoFba2L2iNeSOlnGHtO.1qSU1g4p9uZTBmRTuhQul55KedtK5.', 'user@orasaka.com', true, '{"language":"en", "tts-voice":"nova", "image-aspect-ratio":"1:1", "chat-temperature":0.7}', 'premium'),
('550e8400-e29b-41d4-a716-446655440003', 'guest', '$2a$10$WoQPPrLHNHReFgtodEDE2e8DAkk4zK/ANdmd1lWHnzOXjQRJb.x6O', 'guest@orasaka.com', true, '{"language":"en", "tts-voice":"shimmer", "image-aspect-ratio":"4:3", "chat-temperature":0.9}', 'free')
ON CONFLICT (username) DO NOTHING;

INSERT INTO orasaka_authorities (user_id, authority_name) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'ROLE_ADMIN'),
('550e8400-e29b-41d4-a716-446655440002', 'ROLE_USER'),
('550e8400-e29b-41d4-a716-446655440003', 'ROLE_GUEST')
ON CONFLICT ON CONSTRAINT unique_user_authority DO NOTHING;

INSERT INTO orasaka_rate_limits (tier_key, requests_per_minute, concurrent_jobs) VALUES
('free', 5, 1),
('premium', 50, 5),
('enterprise', 1000, 20)
ON CONFLICT (tier_key) DO NOTHING;

-- ============================================================================
-- 2. CORE & MODELS SEED DATA
-- ============================================================================

INSERT INTO ai_providers (name, base_url, api_key) VALUES
('ollama', 'http://localhost:11434', NULL),
('localai', 'http://localhost:8085', 'not-required'),
('localai-video', 'http://localhost:8188', NULL),
('localai-image', 'http://localhost:8086', 'not-required')
ON CONFLICT (name) DO NOTHING;

INSERT INTO orasaka_models (model_name, model_label, category, options, provider_name, is_default, max_steps, recommended_fps, supported_hardware) VALUES
-- Speech Models
('piper-en-low', 'Piper Low (en)', 'speech', 'Ryan,Low', 'localai', FALSE, NULL, NULL, NULL),
('piper-en-medium-ryan', 'Piper Ryan (en)', 'speech', 'Ryan,Medium', 'localai', TRUE, NULL, NULL, NULL),
('piper-fr-medium', 'Piper Medium (fr)', 'speech', 'Medium,Fr', 'localai', FALSE, NULL, NULL, NULL),
('tts-1', 'OpenAI TTS-1', 'speech', 'Alloy,Echo,Fable,Onyx,Nova,Shimmer', 'localai', FALSE, NULL, NULL, NULL),
-- Image Models
('sdxl-turbo-gguf', 'SDXL Turbo (GGUF)', 'image', NULL, 'localai-image', FALSE, NULL, NULL, NULL),
('sd-1.5-apple-coreml', 'SD 1.5 (Apple CoreML)', 'image', NULL, 'localai-image', FALSE, NULL, NULL, NULL),
('stable-diffusion-xl', 'Stable Diffusion XL', 'image', NULL, 'localai-image', TRUE, NULL, NULL, NULL),
('v1-5-pruned-emaonly', 'SD 1.5 Pruned EMA (safetensors)', 'image', NULL, 'localai-image', FALSE, NULL, NULL, 'APPLE_SILICON_MLX'),
-- Video Models
('stable-video-diffusion-img2vid-xt', 'Stable Video Diffusion XT', 'video', NULL, 'localai-video', TRUE, 25, 14, 'cuda,mps'),
('animatediff-lightning-mps', 'AnimateDiff Lightning (MPS)', 'video', NULL, 'localai-video', FALSE, 8, 12, 'mps'),
('apple-coreml-video-pipeline', 'Apple CoreML Video Pipeline', 'video', NULL, 'localai-video', FALSE, NULL, NULL, 'mps'),
('mlx-animatediff-lightning', 'MLX Native AnimateDiff', 'video', NULL, 'localai-video', FALSE, 8, 12, 'APPLE_SILICON_MLX'),
('mlx-stable-diffusion-video', 'MLX Native Video', 'video', NULL, 'localai-video', FALSE, 25, 14, 'APPLE_SILICON_MLX'),
('stable-video-diffusion-img2vid-xt-mps-fp32', 'SVD XT (PyTorch MPS Float32)', 'video', NULL, 'localai-video', FALSE, 25, 14, 'APPLE_SILICON_MLX'),
-- Vision Models
('llava:latest', 'LLaVA (latest)', 'vision', NULL, 'ollama', FALSE, NULL, NULL, NULL),
('llava:v1.6', 'LLaVA (v1.6)', 'vision', NULL, 'ollama', FALSE, NULL, NULL, NULL),
('bakllava:latest', 'BakLLaVA (latest)', 'vision', NULL, 'ollama', FALSE, NULL, NULL, NULL),
('llama3.2-vision:latest', 'Llama 3.2 Vision (latest)', 'vision', NULL, 'ollama', TRUE, NULL, NULL, NULL),
-- Themes
('rose', 'Rose Accent', 'theme', NULL, 'ollama', FALSE, NULL, NULL, NULL),
('emerald', 'Emerald Accent', 'theme', NULL, 'ollama', FALSE, NULL, NULL, NULL),
('amber', 'Amber Accent', 'theme', NULL, 'ollama', FALSE, NULL, NULL, NULL),
('zinc', 'Zinc Accent', 'theme', NULL, 'ollama', TRUE, NULL, NULL, NULL),
-- Audio Models
('whisper-base', 'Whisper Base', 'audio', NULL, 'ollama', TRUE, NULL, NULL, NULL),
('whisper-tiny-en', 'Whisper Tiny (en)', 'audio', NULL, 'ollama', FALSE, NULL, NULL, NULL),
-- Code Models
('qwen2.5-coder:7b', 'Qwen 2.5 Coder 7B', 'code', 'q4_K_M', 'ollama', TRUE, NULL, NULL, NULL),
('codellama:7b', 'CodeLlama 7B', 'code', 'q4_K_M', 'ollama', FALSE, NULL, NULL, NULL),
-- MLX Native Models (Apple Silicon Unified Memory)
('argmaxinc/mlx-FLUX.1-schnell-4bit-quantized', 'FLUX.1 Schnell 4-bit (MLX)', 'image', NULL, 'localai-image', FALSE, 4, 0, 'APPLE_SILICON_MLX'),
('ByteDance/AnimateDiff-Lightning', 'AnimateDiff Lightning', 'video', NULL, 'localai-video', FALSE, 8, 12, 'APPLE_SILICON_MLX')
ON CONFLICT (model_name) DO NOTHING;

-- Feature Flags
INSERT INTO orasaka_feature_flags (feature_key, is_enabled) VALUES
('orasaka.core.chat.text', true),
('orasaka.core.chat.code', true),
('orasaka.core.chat.image', true),
('orasaka.core.chat.speech', true),
('orasaka.core.media.video', true),
('orasaka.core.media.vision', true),
('orasaka.core.media.audio', true),
('orasaka.core.media.video.analysis', true)
ON CONFLICT (feature_key) DO NOTHING;

-- Platform Tool Config
INSERT INTO platform_tool_configs (tool_id, cache_enabled, cache_ttl_seconds, rag_enabled, chunker_type, source_table) VALUES 
('searchWeb', TRUE, 3600, TRUE, 'MARKDOWN_CHUNKERS', 'orasaka_tools_rag_source')
ON CONFLICT (tool_id) DO NOTHING;



-- ============================================================================
-- 3. TOOLS SEED DATA
-- ============================================================================

INSERT INTO orasaka_tools_rag_source (tool_id, content, metadata) VALUES 
('searchWeb', 'Orasaka Corporation is a powerful mega-corporation specializing in security, banking, and manufacturing.', '{"source":"corp_profile"}'),
('searchWeb', 'The Orasaka core framework runs on Java 21 with Spring AI 1.1.6.', '{"source":"tech_spec"}'),
('searchWeb', 'The BFF proxy routes incoming frontend requests from Next.js to the Orasaka Gateway at http://localhost:8080.', '{"source":"bff_proxy"}')
ON CONFLICT ON CONSTRAINT unique_tool_content DO NOTHING;

-- ============================================================================
-- 4. USER PROFILE SEED DATA (required for profile/settings tests)
-- ============================================================================

INSERT INTO orasaka_user_profiles (user_id, theme, voice_model, primary_industry, ai_behavior, raw_preferences) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'emerald', 'alloy', 'tech', 'professional', '{"language":"en","tts-voice":"alloy"}'),
('550e8400-e29b-41d4-a716-446655440002', 'emerald', 'nova', 'tech', 'friendly', '{"language":"en","tts-voice":"nova"}'),
('550e8400-e29b-41d4-a716-446655440003', 'zinc', 'shimmer', 'general', 'casual', '{"language":"en","tts-voice":"shimmer"}')
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================================
-- 5. PIPELINE INTERCEPTOR CONFIG (required for admin pipeline settings tests)
-- ============================================================================

INSERT INTO pipeline_interceptor_config (interceptor_key, display_label, execution_order, is_enabled, description) VALUES
('userContextResolver',    'User Context Resolver',    1, TRUE,  'Resolves user profile, RBAC, and rate-limiting tier'),
('systemContextInjector',  'System Context Injector',  2, TRUE,  'Injects environment signals, tools, and system variables'),
('ragInterceptor',         'RAG Interceptor',          3, TRUE,  'Vector store retrieval and context injection'),
('mcpInterceptor',         'MCP Interceptor',          4, TRUE,  'External MCP knowledge resolution'),
('memoryInterceptor',      'Memory Interceptor',       5, TRUE,  'Conversation history prepend (FIFO window)'),
('refinerInterceptor',     'Refiner Interceptor',      6, TRUE,  'Fuzzy query to precise instruction refinement'),
('routerInterceptor',      'Router Interceptor',       7, TRUE,  'Intent to optimal provider routing (temp: 0.0)'),
('toolInterceptor',        'Tool Interceptor',         8, TRUE,  'Tool callback attachment (demand-driven)'),
('mediaInterceptor',       'Media Interceptor',        9, TRUE,  'Base64 media extraction and multimodal assembly')
ON CONFLICT (interceptor_key) DO NOTHING;

-- ============================================================================
-- 6. CHAT MODEL (required for chat tests — tinyllama is fast & lightweight)
-- ============================================================================

INSERT INTO orasaka_models (model_name, model_label, category, options, provider_name, is_default) VALUES
('tinyllama:latest', 'TinyLlama (latest)', 'chat', NULL, 'ollama', TRUE)
ON CONFLICT (model_name) DO NOTHING;
