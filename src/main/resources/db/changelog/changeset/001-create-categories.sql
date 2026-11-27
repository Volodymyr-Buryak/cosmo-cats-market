--liquibase formatted sql

--changeset cosmocats:create-categories
CREATE TABLE categories
(
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE CHECK (LENGTH(TRIM(name)) > 0),
    description TEXT NOT NULL CHECK (LENGTH(TRIM(description)) > 0),
    parent_id   UUID REFERENCES categories (id) ON DELETE RESTRICT,

    CONSTRAINT chk_categories_parent CHECK (parent_id <> id)
);

CREATE INDEX idx_categories_parent ON categories (parent_id);
