ALTER TABLE branches
    ADD COLUMN max_concurrent_appointments INTEGER NOT NULL DEFAULT 1;
