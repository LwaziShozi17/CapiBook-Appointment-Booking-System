CREATE TABLE appointments (
    id               UUID NOT NULL,
    customer_id      UUID NOT NULL,
    branch_id        UUID NOT NULL,
    service_id       UUID NOT NULL,
    appointment_date DATE NOT NULL,
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_number VARCHAR(20) NOT NULL,
    notes            TEXT,
    version          INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointments           PRIMARY KEY (id),
    CONSTRAINT uq_appointments_reference UNIQUE (reference_number),
    CONSTRAINT fk_appointments_customer  FOREIGN KEY (customer_id)  REFERENCES users(id),
    CONSTRAINT fk_appointments_branch    FOREIGN KEY (branch_id)    REFERENCES branches(id),
    CONSTRAINT fk_appointments_service   FOREIGN KEY (service_id)   REFERENCES banking_services(id)
);

CREATE INDEX idx_appointments_branch_date ON appointments (branch_id, appointment_date);
