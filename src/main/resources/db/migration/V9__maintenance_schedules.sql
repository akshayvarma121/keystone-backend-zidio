CREATE TABLE maintenance_schedules (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    customer_id UUID NOT NULL REFERENCES customers(id),
    site_id UUID NOT NULL REFERENCES sites(id),
    required_skill_id UUID REFERENCES skills(id),
    frequency_days INTEGER NOT NULL CHECK (frequency_days > 0),
    next_run_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
