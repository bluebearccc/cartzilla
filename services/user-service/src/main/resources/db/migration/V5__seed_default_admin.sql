-- V5__seed_default_admin.sql
-- Tạo tài khoản admin mặc định nếu chưa tồn tại.
-- Email    : admin@cartzilla.com
-- Password : Admin@123456   (BCrypt strength 10)
-- ĐỔI MẬT KHẨU SAU KHI ĐĂNG NHẬP LẦN ĐẦU khi deploy production!

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
    'admin@cartzilla.com',
    '$2a$10$zOCIwnaja6JAmah2NEmTCugaLm.7fLE8QqNa337XxSQ7hC22FqEh6',
    'System Admin',
    NULL,
    'ADMIN',
    true,
    true,
    NOW(),
    false
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@cartzilla.com'
);
