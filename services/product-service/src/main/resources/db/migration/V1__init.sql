-- product-service schema

CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id       UUID REFERENCES categories(id),
    name            VARCHAR(100)  NOT NULL,
    slug            VARCHAR(120)  NOT NULL UNIQUE,
    image_url       TEXT,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_cat_parent    ON categories(parent_id);
CREATE INDEX idx_cat_slug      ON categories(slug);

CREATE TABLE vendors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150)  NOT NULL,
    slug            VARCHAR(160)  NOT NULL UNIQUE,
    vendor_type     VARCHAR(20)   NOT NULL,
    contact_email   VARCHAR(255),
    phone           VARCHAR(20),
    website         TEXT,
    logo_url        TEXT,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id     UUID          NOT NULL REFERENCES categories(id),
    vendor_id       UUID          REFERENCES vendors(id),
    name            VARCHAR(200)  NOT NULL,
    slug            VARCHAR(220)  NOT NULL UNIQUE,
    description     TEXT,
    base_price      NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),
    tags            TEXT,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    featured        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_prod_category ON products(category_id);
CREATE INDEX idx_prod_vendor   ON products(vendor_id);
CREATE INDEX idx_prod_slug     ON products(slug);
CREATE INDEX idx_prod_active   ON products(active) WHERE is_deleted = FALSE;

CREATE TABLE product_variants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID          NOT NULL REFERENCES products(id),
    sku             VARCHAR(50)   NOT NULL UNIQUE,
    size            VARCHAR(20),
    color           VARCHAR(50),
    color_hex       VARCHAR(10),
    price           NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock           INT           NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_var_product   ON product_variants(product_id);
CREATE INDEX idx_var_sku       ON product_variants(sku);

CREATE TABLE product_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID          NOT NULL REFERENCES products(id),
    image_url       TEXT          NOT NULL,
    alt_text        VARCHAR(200),
    is_primary      BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order      INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_img_product   ON product_images(product_id);
