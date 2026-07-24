-- Update demo accounts to match the frontend login credentials
-- Manager: sarah@keystone.io / manage123
UPDATE users SET name = 'Sarah Jenkins', email = 'sarah@keystone.io',
  password_hash = '$2b$10$iLIXoQLSkNtr/SAuDLroju97lGCb71KoAnUCnYQ4EhPLLLUZdCjYa'
WHERE id = '33333333-3333-3333-3333-333333333331';

-- Dispatcher: dispatch@keystone.io / route456
UPDATE users SET name = 'Marcus Vance', email = 'dispatch@keystone.io',
  password_hash = '$2b$10$b7eLykIEyce6e6FHkhc.U.zLeXYqyFRmAUd5faK2LIipIp2AyFSoy'
WHERE id = '33333333-3333-3333-3333-333333333332';

-- Technician: fieldops@keystone.io / wrench789
UPDATE users SET name = 'David Reynolds', email = 'fieldops@keystone.io',
  password_hash = '$2b$10$uLuD5MjbsdX3QtgjbnNZFOxqP36qiUQ8IZGGcF5W7r9/6l82ZBbrq'
WHERE id = '33333333-3333-3333-3333-333333333333';

-- Customer: samantha@apexhq.com / tenant321
UPDATE users SET name = 'Samantha Wright', email = 'samantha@apexhq.com',
  password_hash = '$2b$10$IjTQCnNLu8e75NlwDiFjLOmDTVAz0Iil0RNPZA2AoxyYdySU2SizO'
WHERE id = '33333333-3333-3333-3333-333333333334';

-- Update customer company name to match frontend
UPDATE customers SET name = 'Apex HQ', contact_email = 'samantha@apexhq.com'
WHERE id = '11111111-1111-1111-1111-111111111111';
