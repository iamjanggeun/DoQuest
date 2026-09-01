ALTER TABLE memo_analyses
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error VARCHAR(500);

ALTER TABLE schedules
    ADD CONSTRAINT uk_schedules_memo UNIQUE (memo_id);
