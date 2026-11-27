--liquibase formatted sql

--changeset cosmocats:create-products
CREATE TABLE products
(
    id             UUID PRIMARY KEY,
    name           TEXT       NOT NULL CHECK (LENGTH(TRIM(name)) > 0),
    description    TEXT       NOT NULL CHECK (LENGTH(TRIM(description)) > 0),
    price_amount   NUMERIC    NOT NULL CHECK (price_amount > 0),
    price_currency VARCHAR(3) NOT NULL CHECK (REGEXP_LIKE(price_currency, '^[A-Z]{3}$')),
    stock          INTEGER    NOT NULL CHECK (stock >= 0),
    category_id    UUID       NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,

    CONSTRAINT uq_products_category_name UNIQUE (category_id, name),
    CONSTRAINT chk_products_finite_price CHECK (CAST(price_amount AS VARCHAR) NOT IN ('NaN', 'Infinity', '-Infinity'))
);

--changeset cosmocats:create-product-indexes
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_price ON products (price_amount);
