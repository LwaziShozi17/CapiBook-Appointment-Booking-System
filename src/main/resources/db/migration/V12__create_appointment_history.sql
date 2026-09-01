CREATE TABLE appointment_history (
    id               UUID NOT NULL,
    appointment_id   UUID NOT NULL,
    previous_status  VARCHAR(20) NOT NULL,
    new_status       VARCHAR(20) NOT NULL,
    changed_by_id    UUID NOT NULL,
    change_reason    VARCHAR(500),
    changed_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointment_history              PRIMARY KEY (id),
    CONSTRAINT fk_appointment_history_appointment  FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_appointment_history_changed_by   FOREIGN KEY (changed_by_id)  REFERENCES users(id)
);

CREATE INDEX idx_appointment_history_appointment_id ON appointment_history (appointment_id);
