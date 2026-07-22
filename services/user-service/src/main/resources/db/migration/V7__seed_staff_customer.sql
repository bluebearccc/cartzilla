-- V7__seed_staff_customer.sql
-- Tạo tài khoản demo cho STAFF và CUSTOMER nếu chưa tồn tại (bổ sung cho ADMIN ở V5).
-- Email    : staff@cartzilla.com    | Password : Staff@123456     (BCrypt strength 10)
-- Email    : customer@cartzilla.com | Password : Customer@123456  (BCrypt strength 10)
-- CHỈ DÙNG CHO LOCAL/DEMO. ĐỔI MẬT KHẨU TRƯỚC KHI DÙNG NGOÀI MÔI TRƯỜNG DEV!

INSERT INTO users (
    id,
    email,
    password_hash,
    full_name,
    phone,
    role,
    email_verified,
    is_active,
    created_at,
    is_deleted
)
SELECT
    gen_random_uuid(),
    'staff@cartzilla.com',
    '$2a$10$o4t.zPRDJhqTJjLguylQZeCV8F1c2N/swczhTdb/W7TaTz5Sr8MfC',
    'Demo Staff',
    NULL,
    'STAFF',
    true,
    true,
    NOW(),
    false
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'staff@cartzilla.com'
);

INSERT INTO users (
    id,
    email,
    password_hash,
    full_name,
    phone,
    role,
    email_verified,
    is_active,
    created_at,
    is_deleted
)
SELECT
    gen_random_uuid(),
    'customer@cartzilla.com',
    '$2a$10$k2fdsIb1QFJ2P7InGeV7w.WlOE/guV02oiD1QbzgHrJ2e3ucmW5z.',
    'Demo Customer',
    NULL,
    'CUSTOMER',
    true,
    true,
    NOW(),
    false
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'customer@cartzilla.com'
);
