ALTER TABLE work_orders ADD COLUMN search_vector TSVECTOR;

CREATE OR REPLACE FUNCTION work_orders_search_vector_trigger() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('english', coalesce(NEW.code,'')), 'A') ||
    setweight(to_tsvector('english', coalesce(NEW.title,'')), 'B') ||
    setweight(to_tsvector('english', coalesce(NEW.description,'')), 'C');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
ON work_orders FOR EACH ROW EXECUTE FUNCTION work_orders_search_vector_trigger();

-- Backfill existing rows (will fire the trigger)
UPDATE work_orders SET id = id;

CREATE INDEX idx_work_orders_search ON work_orders USING GIN(search_vector);
