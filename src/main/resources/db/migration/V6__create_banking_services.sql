CREATE TABLE banking_services (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_banking_services PRIMARY KEY (id),
    CONSTRAINT uq_banking_services_name UNIQUE (name),
    CONSTRAINT chk_banking_services_duration CHECK (duration_minutes > 0)
);
