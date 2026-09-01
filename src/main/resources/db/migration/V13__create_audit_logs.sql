-- Audit log table — records every appointment lifecycle event received from Kafka.
-- actor_id stores the UUID of the user who triggered the event.
-- No FK constraint on actor_id: events arrive asynchronously and the user may
-- have been deactivated between event publication and consumption.
CREATE TABLE audit_logs (
    id          UUID         PRIMARY KEY,
    actor_id    UUID,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   VARCHAR(100),
    details     TEXT,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
