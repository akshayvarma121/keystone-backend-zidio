-- 1 Customer
INSERT INTO customers (id, name, contact_email) VALUES ('11111111-1111-1111-1111-111111111111', 'Acme Corp', 'contact@acmecorp.com');

-- 2 Sites
INSERT INTO sites (id, customer_id, name, address) VALUES ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 'Acme HQ', '123 Acme Way');
INSERT INTO sites (id, customer_id, name, address) VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Acme Warehouse', '456 Industrial Blvd');

-- 4 Users (manager, dispatcher, technician, customer) - password is Password123!
INSERT INTO users (id, name, email, password_hash, role, customer_id, active) VALUES 
('33333333-3333-3333-3333-333333333331', 'Alice Manager', 'alice@keystone.local', '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy', 'MANAGER', NULL, TRUE),
('33333333-3333-3333-3333-333333333332', 'Bob Dispatcher', 'bob@keystone.local', '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy', 'DISPATCHER', NULL, TRUE),
('33333333-3333-3333-3333-333333333333', 'Charlie Tech', 'charlie@keystone.local', '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy', 'TECHNICIAN', NULL, TRUE),
('33333333-3333-3333-3333-333333333334', 'Dave Customer', 'dave@acmecorp.com', '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy', 'CUSTOMER', '11111111-1111-1111-1111-111111111111', TRUE);

-- 5 Parts
INSERT INTO parts (id, name, sku, unit_cost, stock_qty) VALUES 
('44444444-4444-4444-4444-444444444441', 'Compressor Motor', 'COMP-M-001', 250.00, 15),
('44444444-4444-4444-4444-444444444442', 'Air Filter', 'AF-200X', 15.50, 100),
('44444444-4444-4444-4444-444444444443', 'Coolant 1Gal', 'COOL-1G', 25.00, 50),
('44444444-4444-4444-4444-444444444444', 'Thermostat', 'THERM-V2', 75.00, 30),
('44444444-4444-4444-4444-444444444445', 'Copper Tubing 10ft', 'TUBE-CU-10', 40.00, 40);
