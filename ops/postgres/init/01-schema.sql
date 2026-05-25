-- ============================================================================
-- 1. CLEANUP: PURGE ANCIENNES TABLES (Ordre strict pour éviter les conflits de FK)
-- ============================================================================
DROP TABLE IF EXISTS orasaka_user_interceptions CASCADE;
DROP TABLE IF EXISTS orasaka_verification_tokens CASCADE;
DROP TABLE IF EXISTS orasaka_authorities CASCADE;
DROP TABLE IF EXISTS orasaka_tools_rag_source CASCADE;
DROP TABLE IF EXISTS orasaka_tools_cache CASCADE;
DROP TABLE IF EXISTS orasaka_users CASCADE;
DROP TABLE IF EXISTS orasaka_rate_limit_tiers CASCADE;

-- ============================================================================
-- 2. SCHÉMAS CORE & INFRASTRUCTURE
-- ============================================================================

-- Tiers de Rate Limiting
CREATE TABLE orasaka_rate_limit_tiers (
    id VARCHAR(50) PRIMARY KEY,
    capacity INT NOT NULL,
    refill_tokens INT NOT NULL,
    refill_seconds INT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table Utilisateurs (Contient la colonne rate_limit_tier alignée)
CREATE TABLE orasaka_users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    preferences TEXT,
    provider VARCHAR(50) DEFAULT 'local' NOT NULL,
    provider_id VARCHAR(255) DEFAULT NULL,
    rate_limit_tier VARCHAR(50) REFERENCES orasaka_rate_limit_tiers(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_provider_user UNIQUE (provider, provider_id)
);

-- Rôles et Autorisations
CREATE TABLE orasaka_authorities (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    authority_name VARCHAR(100) NOT NULL,
    CONSTRAINT unique_user_authority UNIQUE (user_id, authority_name)
);

CREATE INDEX idx_authorities_user ON orasaka_authorities(user_id);

-- Tokens de Vérification (Email, Onboarding)
CREATE TABLE orasaka_verification_tokens (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    token_type VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expiry_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Interceptions d'États Agents
CREATE TABLE orasaka_user_interceptions (
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    interception_type VARCHAR(100) NOT NULL,
    schema_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, interception_type)
);

-- ============================================================================
-- 3. SCHÉMAS OUTILS & RAG (MODULE: ORASAKA-TOOLS)
-- ============================================================================

-- Cache des fonctions et outils LLM
CREATE TABLE orasaka_tools_cache (
    tool_id VARCHAR(255) NOT NULL,
    cache_key TEXT NOT NULL,
    cache_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (tool_id, cache_key)
);

-- Sources documentaires pour le RAG
CREATE TABLE orasaka_tools_rag_source (
    id SERIAL PRIMARY KEY,
    tool_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    ingested BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_tool_content UNIQUE (tool_id, content)
);