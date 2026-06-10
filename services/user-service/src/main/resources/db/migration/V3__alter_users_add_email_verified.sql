ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Alter vouchers table to add missing fields from Voucher entity
ALTER TABLE vouchers ADD COLUMN max_discount_amount DECIMAL(12,2);
ALTER TABLE vouchers ADD COLUMN min_account_age_days INT NOT NULL DEFAULT 0;
ALTER TABLE vouchers ADD COLUMN per_user_limit INT NOT NULL DEFAULT 1;
ALTER TABLE vouchers ADD COLUMN audience_type VARCHAR(30) NOT NULL DEFAULT 'ALL_USERS';
ALTER TABLE vouchers ADD COLUMN first_order_only BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE vouchers ADD COLUMN min_completed_orders INT NOT NULL DEFAULT 0;
ALTER TABLE vouchers ADD COLUMN min_total_spent DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE vouchers ADD COLUMN starts_at TIMESTAMP;
