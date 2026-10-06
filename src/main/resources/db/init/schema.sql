CREATE TABLE IF NOT EXISTS products
(
    id              UUID PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    description     VARCHAR(500)   NOT NULL,
    price           DECIMAL(19, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    stock           INTEGER        NOT NULL,
    category_id_ref UUID           NOT NULL,

    CONSTRAINT chk_product_name_length CHECK (LENGTH(TRIM(name)) >= 3),
    CONSTRAINT chk_product_description_not_blank CHECK (LENGTH(TRIM(description)) > 0),
    CONSTRAINT chk_product_price CHECK (price > 0),
    CONSTRAINT chk_product_stock CHECK (stock >= 0)
);

CREATE INDEX IF NOT EXISTS idx_products_category_id_ref
    ON products (category_id_ref);

CREATE INDEX IF NOT EXISTS idx_products_price
    ON products (price);
