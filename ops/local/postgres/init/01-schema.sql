-- ============================================================================
-- 1. CLEANUP: PURGE ALL OLD TABLES (Cascaded to prevent FK issues)
-- ============================================================================

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS CASCADE;
DROP TABLE IF EXISTS QRTZ_CALENDARS CASCADE;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS CASCADE;
DROP TABLE IF EXISTS QRTZ_LOCKS CASCADE;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE CASCADE;

DROP TABLE IF EXISTS automation_job_execution_log CASCADE;
DROP TABLE IF EXISTS connector_credentials CASCADE;

DROP TABLE IF EXISTS pipeline_interceptor_config CASCADE;
DROP TABLE IF EXISTS platform_tool_configs CASCADE;
DROP TABLE IF EXISTS platform_mcp_servers CASCADE;
DROP TABLE IF EXISTS ai_providers CASCADE;
DROP TABLE IF EXISTS orasaka_chat_sessions CASCADE;
DROP TABLE IF EXISTS orasaka_models CASCADE;
DROP TABLE IF EXISTS orasaka_feature_flags CASCADE;
DROP TABLE IF EXISTS orasaka_rate_limits CASCADE;
DROP TABLE IF EXISTS orasaka_jobs CASCADE;

DROP TABLE IF EXISTS user_mcp_servers CASCADE;
DROP TABLE IF EXISTS orasaka_password_resets CASCADE;
DROP TABLE IF EXISTS orasaka_ai_mcp_servers CASCADE;
DROP TABLE IF EXISTS orasaka_ai_rag_stores CASCADE;
DROP TABLE IF EXISTS user_credentials CASCADE;
DROP TABLE IF EXISTS orasaka_user_profiles CASCADE;
DROP TABLE IF EXISTS orasaka_user_interceptions CASCADE;
DROP TABLE IF EXISTS orasaka_verification_tokens CASCADE;
DROP TABLE IF EXISTS orasaka_authorities CASCADE;
DROP TABLE IF EXISTS orasaka_users CASCADE;
DROP TABLE IF EXISTS orasaka_rate_limit_tiers CASCADE;

DROP TABLE IF EXISTS orasaka_tools_cache CASCADE;
DROP TABLE IF EXISTS orasaka_tools_rag_source CASCADE;

-- ============================================================================
-- 2. IDENTITY DOMAIN SCHEMA
-- ============================================================================

CREATE TABLE orasaka_rate_limit_tiers (
    id VARCHAR(50) PRIMARY KEY,
    capacity INT NOT NULL,
    refill_tokens INT NOT NULL,
    refill_seconds INT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

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
    password_changed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT unique_provider_user UNIQUE (provider, provider_id)
);

CREATE TABLE orasaka_authorities (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    authority_name VARCHAR(100) NOT NULL,
    CONSTRAINT unique_user_authority UNIQUE (user_id, authority_name)
);
CREATE INDEX idx_authorities_user ON orasaka_authorities(user_id);

CREATE TABLE orasaka_verification_tokens (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    token_type VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expiry_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orasaka_user_interceptions (
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    interception_type VARCHAR(100) NOT NULL,
    schema_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, interception_type)
);

CREATE TABLE orasaka_user_profiles (
    user_id VARCHAR(255) PRIMARY KEY REFERENCES orasaka_users(id) ON DELETE CASCADE,
    theme VARCHAR(50) DEFAULT 'emerald',
    voice_model VARCHAR(50) DEFAULT 'alloy',
    primary_industry VARCHAR(100) DEFAULT 'tech',
    ai_behavior TEXT,
    raw_preferences TEXT
);

CREATE TABLE user_credentials (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES orasaka_users(id) ON DELETE CASCADE,
    provider_name VARCHAR(255) NOT NULL,
    api_key VARCHAR(1024) NOT NULL
);

CREATE TABLE orasaka_ai_mcp_servers (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES orasaka_users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orasaka_ai_rag_stores (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES orasaka_users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    store_type VARCHAR(50) NOT NULL,
    host VARCHAR(255),
    port INT,
    database_name VARCHAR(255),
    table_name VARCHAR(255),
    username VARCHAR(255),
    password VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orasaka_password_resets (
    id         VARCHAR(255) PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_resets_token_hash ON orasaka_password_resets(token_hash);
CREATE INDEX idx_password_resets_email ON orasaka_password_resets(email);

CREATE TABLE user_mcp_servers (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES orasaka_users(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    auth_token VARCHAR(1000),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_mcp_servers_user ON user_mcp_servers(user_id);

-- ============================================================================
-- 3. CORE DOMAIN SCHEMA
-- ============================================================================

CREATE TABLE orasaka_jobs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255),
    feature_key VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload JSONB,
    result JSONB,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_orasaka_jobs_user_id ON orasaka_jobs(user_id);

CREATE TABLE orasaka_rate_limits (
    tier_key VARCHAR(50) PRIMARY KEY,
    requests_per_minute INT NOT NULL,
    concurrent_jobs INT NOT NULL
);

CREATE TABLE orasaka_feature_flags (
    feature_key VARCHAR(255) PRIMARY KEY,
    is_enabled BOOLEAN NOT NULL
);

CREATE TABLE orasaka_models (
    id SERIAL PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL UNIQUE,
    model_label VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    options VARCHAR(1000),
    is_default BOOLEAN DEFAULT FALSE,
    provider_name VARCHAR(255) DEFAULT 'ollama',
    max_steps INT,
    recommended_fps INT,
    supported_hardware VARCHAR(255)
);

CREATE TABLE orasaka_chat_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES orasaka_users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_providers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    base_url VARCHAR(255) NOT NULL,
    api_key VARCHAR(255)
);

CREATE TABLE platform_mcp_servers (
    id SERIAL PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    transport_type VARCHAR(50) NOT NULL,
    url VARCHAR(1000),
    command VARCHAR(1000),
    args VARCHAR(2000),
    auth_token VARCHAR(1000),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE platform_tool_configs (
    id SERIAL PRIMARY KEY,
    tool_id VARCHAR(255) NOT NULL UNIQUE,
    cache_enabled BOOLEAN DEFAULT TRUE,
    cache_ttl_seconds INT DEFAULT 3600,
    rag_enabled BOOLEAN DEFAULT TRUE,
    chunker_type VARCHAR(100) DEFAULT 'MARKDOWN_CHUNKERS',
    source_table VARCHAR(255) DEFAULT 'orasaka_tools_rag_source',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pipeline_interceptor_config (
    id              SERIAL       PRIMARY KEY,
    interceptor_key VARCHAR(100) NOT NULL UNIQUE,
    display_label   VARCHAR(200) NOT NULL,
    execution_order INTEGER      NOT NULL DEFAULT 0,
    is_enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    description     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_pipeline_interceptor_config_key ON pipeline_interceptor_config (interceptor_key);
CREATE INDEX idx_pipeline_interceptor_config_order ON pipeline_interceptor_config (execution_order);

-- ============================================================================
-- 4. TOOLS SCHEMA
-- ============================================================================

CREATE TABLE orasaka_tools_cache (
    tool_id VARCHAR(255) NOT NULL,
    cache_key TEXT NOT NULL,
    cache_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (tool_id, cache_key)
);

CREATE TABLE orasaka_tools_rag_source (
    id SERIAL PRIMARY KEY,
    tool_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    ingested BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(255) REFERENCES orasaka_users(id) ON DELETE CASCADE,
    CONSTRAINT unique_tool_content UNIQUE (tool_id, content)
);
CREATE INDEX idx_tools_rag_source_user ON orasaka_tools_rag_source(user_id);

-- ============================================================================
-- 5. AUTOMATION WORKER SCHEMA
-- ============================================================================

CREATE TABLE connector_credentials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         VARCHAR(255) NOT NULL,
    connector_type  VARCHAR(50)  NOT NULL,
    credential_key  VARCHAR(255) NOT NULL,
    encrypted_value TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_connector_credentials_user_type_key UNIQUE (user_id, connector_type, credential_key)
);
CREATE INDEX idx_connector_credentials_user_type ON connector_credentials(user_id, connector_type);

CREATE TABLE automation_job_execution_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id           VARCHAR(255) NOT NULL,
    user_id          VARCHAR(255) NOT NULL,
    connector_type   VARCHAR(50)  NOT NULL,
    action           VARCHAR(255) NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    payload          JSONB,
    result           JSONB,
    error_message    TEXT,
    started_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMP,
    duration_ms      BIGINT
);
CREATE INDEX idx_job_exec_log_job_id ON automation_job_execution_log(job_id);
CREATE INDEX idx_job_exec_log_user_status ON automation_job_execution_log(user_id, status);
CREATE INDEX idx_job_exec_log_started_at ON automation_job_execution_log(started_at);

-- ============================================================================
-- 6. QUARTZ SCHEMA
-- ============================================================================

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250),
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOLEAN      NOT NULL,
    IS_NONCONCURRENT  BOOLEAN      NOT NULL,
    IS_UPDATE_DATA    BOOLEAN      NOT NULL,
    REQUESTS_RECOVERY BOOLEAN      NOT NULL,
    JOB_DATA          BYTEA,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250),
    NEXT_FIRE_TIME BIGINT,
    PREV_FIRE_TIME BIGINT,
    PRIORITY       INTEGER,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT,
    CALENDAR_NAME  VARCHAR(200),
    MISFIRE_INSTR  SMALLINT,
    JOB_DATA       BYTEA,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200),
    JOB_GROUP         VARCHAR(200),
    IS_NONCONCURRENT  BOOLEAN,
    REQUESTS_RECOVERY BOOLEAN,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX idx_qrtz_t_next_fire_time ON QRTZ_TRIGGERS(NEXT_FIRE_TIME);
CREATE INDEX idx_qrtz_t_state ON QRTZ_TRIGGERS(TRIGGER_STATE);
CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);