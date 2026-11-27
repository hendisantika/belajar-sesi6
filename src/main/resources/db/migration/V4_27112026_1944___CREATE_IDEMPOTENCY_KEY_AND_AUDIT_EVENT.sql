-- =====================================================================
-- Idempotency key (posting & reversal) dan audit event terstruktur
-- Audit event: siapa (actor), kapan (occurred_at), apa (action), referensi
-- (entity_type/entity_id/entity_ref), hasil (outcome), before/after (jsonb).
-- =====================================================================

CREATE TABLE idempotency_key
(
    idem_key        VARCHAR(100) PRIMARY KEY,
    operation       VARCHAR(50)  NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_idempotency_key_expires ON idempotency_key (expires_at);

CREATE TABLE audit_event
(
    id             BIGSERIAL PRIMARY KEY,
    event_id       UUID         NOT NULL,
    action         VARCHAR(60)  NOT NULL,
    outcome        VARCHAR(10)  NOT NULL,
    entity_type    VARCHAR(50)  NOT NULL,
    entity_id      BIGINT,
    entity_ref     VARCHAR(100),
    actor          VARCHAR(100) NOT NULL,
    actor_roles    VARCHAR(200),
    reason         VARCHAR(500),
    error_code     VARCHAR(60),
    before_state   JSONB,
    after_state    JSONB,
    correlation_id VARCHAR(64),
    client_ip      VARCHAR(64),
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_audit_event_event_id UNIQUE (event_id),
    CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS', 'REJECTED', 'DENIED')),
    CONSTRAINT ck_audit_event_rejected_reason CHECK (outcome = 'SUCCESS' OR reason IS NOT NULL)
);

CREATE INDEX idx_audit_event_entity ON audit_event (entity_type, entity_id, occurred_at);
CREATE INDEX idx_audit_event_ref ON audit_event (entity_ref);
CREATE INDEX idx_audit_event_occurred ON audit_event (occurred_at);
CREATE INDEX idx_audit_event_actor ON audit_event (actor, occurred_at);
CREATE INDEX idx_audit_event_action ON audit_event (action, outcome);

COMMENT ON TABLE audit_event IS 'Audit trail append-only: tidak boleh di-UPDATE / DELETE oleh aplikasi';
COMMENT ON COLUMN audit_event.outcome IS 'SUCCESS, REJECTED (aturan bisnis menolak), DENIED (otorisasi menolak)';
