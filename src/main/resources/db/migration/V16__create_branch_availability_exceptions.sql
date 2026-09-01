CREATE TABLE branch_availability_exceptions (
    id             UUID         PRIMARY KEY,
    branch_id      UUID         NOT NULL REFERENCES branches(id),
    exception_date DATE         NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    reason         VARCHAR(255),
    created_at     TIMESTAMP    NOT NULL,
    UNIQUE (branch_id, exception_date)
);

CREATE INDEX idx_bae_branch_date ON branch_availability_exceptions (branch_id, exception_date);
