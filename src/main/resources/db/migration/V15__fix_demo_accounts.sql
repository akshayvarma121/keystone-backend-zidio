-- Force update demo accounts again to ensure the $2a$ prefix is applied. 
-- Modifying an existing migration (V14) causes Flyway checksum errors or gets skipped,
-- so this V15 migration guarantees the live Render database applies the fix.

-- Manager: sarah@keystone.io / Password123!
UPDATE users SET name = 'Sarah Jenkins', email = 'sarah@keystone.io',
  password_hash = '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy'
WHERE id = '33333333-3333-3333-3333-333333333331';

-- Dispatcher: dispatch@keystone.io / Password123!
UPDATE users SET name = 'Marcus Vance', email = 'dispatch@keystone.io',
  password_hash = '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy'
WHERE id = '33333333-3333-3333-3333-333333333332';

-- Technician: fieldops@keystone.io / Password123!
UPDATE users SET name = 'David Reynolds', email = 'fieldops@keystone.io',
  password_hash = '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy'
WHERE id = '33333333-3333-3333-3333-333333333333';

-- Customer: samantha@apexhq.com / Password123!
UPDATE users SET name = 'Samantha Wright', email = 'samantha@apexhq.com',
  password_hash = '$2a$12$rIK1ft2bXsdxB4Gq/AGn..N8W/JRdbBQZ3peXem0EAlI/BFBO9tjy'
WHERE id = '33333333-3333-3333-3333-333333333334';
