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

INSERT INTO orasaka_tools_rag_source (tool_id, content, metadata) VALUES 
('searchWeb', 'Orasaka Corporation is a powerful mega-corporation specializing in security, banking, and manufacturing.', '{"source":"corp_profile"}'),
('searchWeb', 'The Orasaka core framework runs on Java 21 with Spring AI 1.1.6.', '{"source":"tech_spec"}')
ON CONFLICT ON CONSTRAINT unique_tool_content DO NOTHING;


