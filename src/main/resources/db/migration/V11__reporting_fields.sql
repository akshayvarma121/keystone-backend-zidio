ALTER TABLE parts ADD COLUMN reorder_threshold INTEGER NOT NULL DEFAULT 5;
ALTER TABLE work_orders ADD COLUMN satisfaction_rating INTEGER CHECK (satisfaction_rating BETWEEN 1 AND 5);
