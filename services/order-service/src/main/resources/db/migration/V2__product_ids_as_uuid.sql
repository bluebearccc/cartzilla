ALTER TABLE cart_items
    ALTER COLUMN product_id TYPE UUID USING 
        CASE 
            WHEN product_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' 
            THEN product_id::uuid 
            ELSE '00000000-0000-0000-0000-000000000000'::uuid 
        END;

ALTER TABLE order_items
    ALTER COLUMN product_id TYPE UUID USING 
        CASE 
            WHEN product_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' 
            THEN product_id::uuid 
            ELSE '00000000-0000-0000-0000-000000000000'::uuid 
        END;
