CREATE TABLE branch_operating_hours (
    id UUID NOT NULL,
    branch_id UUID NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    open_time TIME,
    close_time TIME,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_branch_operating_hours PRIMARY KEY (id),
    CONSTRAINT uq_branch_day UNIQUE (branch_id, day_of_week),
    CONSTRAINT fk_boh_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);
