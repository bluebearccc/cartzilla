ALTER TABLE cart_items ALTER COLUMN product_id TYPE UUID USING product_id::UUID;
ALTER TABLE order_items ALTER COLUMN product_id TYPE UUID USING product_id::UUID;
