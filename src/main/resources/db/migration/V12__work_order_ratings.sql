CREATE TABLE work_order_ratings (
    id UUID PRIMARY KEY,
    work_order_id UUID UNIQUE NOT NULL REFERENCES work_orders(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
