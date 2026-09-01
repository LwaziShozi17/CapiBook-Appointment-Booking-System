CREATE TABLE notifications (
    id              UUID         PRIMARY KEY,
    appointment_id  UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    sent_at         TIMESTAMP,
    error_message   VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_notifications_appointment_id ON notifications (appointment_id);
