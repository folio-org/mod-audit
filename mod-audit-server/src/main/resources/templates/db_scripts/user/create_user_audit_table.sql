CREATE TABLE IF NOT EXISTS user_audit (
    event_id     UUID      PRIMARY KEY,
    event_date   TIMESTAMP NOT NULL,
    user_id      UUID      NOT NULL,
    action       VARCHAR   NOT NULL,
    performed_by UUID,
    diff         JSONB
);
CREATE INDEX IF NOT EXISTS idx_user_audit_user_id_event_date ON user_audit USING BTREE (user_id, event_date DESC);
CREATE INDEX IF NOT EXISTS idx_user_audit_event_date ON user_audit USING BTREE (event_date);
