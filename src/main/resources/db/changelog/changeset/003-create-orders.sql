--liquibase formatted sql

--changeset cosmocats:create-orders
CREATE TABLE orders
(
    id         UUID PRIMARY KEY,
    buyer_id   UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status     VARCHAR(16)              NOT NULL,

    CONSTRAINT chk_orders_status CHECK (status IN ('CREATED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_orders_buyer_created ON orders (buyer_id, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

--changeset cosmocats:create-order-items
CREATE SEQUENCE order_items_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE order_items
(
    id                  BIGINT DEFAULT nextval('order_items_id_seq') PRIMARY KEY,
    order_id            UUID       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    position            INTEGER    NOT NULL CHECK (position >= 0),
    product_id          UUID       NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    product_name        TEXT       NOT NULL CHECK (LENGTH(TRIM(product_name)) > 0),
    unit_price_amount   NUMERIC    NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL CHECK (REGEXP_LIKE(unit_price_currency, '^[A-Z]{3}$')),
    quantity            INTEGER    NOT NULL CHECK (quantity > 0),


    CONSTRAINT uq_order_items_position UNIQUE (order_id, position),
    CONSTRAINT chk_order_items_price CHECK (
        unit_price_amount > 0 AND
        CAST(unit_price_amount AS VARCHAR) NOT IN ('NaN', 'Infinity', '-Infinity')
        )
);

CREATE INDEX idx_order_items_product ON order_items (product_id);

--changeset cosmocats:normalize-order-currency
--preconditions onFail:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM orders o WHERE (SELECT COUNT(DISTINCT i.unit_price_currency) FROM order_items i WHERE i.order_id = o.id) <> 1
ALTER TABLE orders ADD COLUMN currency VARCHAR(3) CHECK (REGEXP_LIKE(currency, '^[A-Z]{3}$'));

UPDATE orders
SET currency = (SELECT MIN(i.unit_price_currency) FROM order_items i WHERE i.order_id = orders.id);

ALTER TABLE orders ALTER COLUMN currency SET NOT NULL;
ALTER TABLE order_items DROP COLUMN unit_price_currency;
