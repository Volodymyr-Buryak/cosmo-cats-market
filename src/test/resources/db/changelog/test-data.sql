--liquibase formatted sql

--changeset cosmocats:test-seed-categories
INSERT INTO categories (id, name, description)
SELECT CAST(c.id AS UUID), c.name, c.description
FROM (VALUES ('40000000-0000-4000-8000-000000000001', 'Test food', 'Catalog category for Test food'),
             ('40000000-0000-4000-8000-000000000002', 'Test toys',
              'Catalog category for Test toys')) AS c(id, name, description)
WHERE NOT EXISTS (SELECT 1
                  FROM categories existing
                  WHERE existing.id = CAST(c.id AS UUID));

--changeset cosmocats:test-seed-products
INSERT INTO products (id, name, description, price_amount, price_currency, stock, category_id)
SELECT CAST(p.id AS UUID), p.name, p.description, p.price, p.currency, p.stock, CAST(p.category_id AS UUID)
FROM (VALUES ('30000000-0000-4000-8000-000000000002', 'Galaxy Bowl', 'A galaxy bowl for repository integration tests.',
              30.00, 'USD', 0, '40000000-0000-4000-8000-000000000001'),
             ('30000000-0000-4000-8000-000000000004', 'Star Blanket',
              'A warm star blanket for repository integration tests.', 20.00, 'USD', 12,
              '40000000-0000-4000-8000-000000000002'),
             ('30000000-0000-4000-8000-000000000001', 'Star Lamp',
              'A compact star lamp for repository integration tests.', 10.00, 'USD', 5,
              '40000000-0000-4000-8000-000000000001'),
             ('30000000-0000-4000-8000-000000000003', 'Comet Toy',
              'A durable comet toy for repository integration tests.', 20.00, 'USD', 8,
              '40000000-0000-4000-8000-000000000002')) AS p(id, name, description, price, currency, stock, category_id)
WHERE NOT EXISTS (SELECT 1 FROM products existing WHERE existing.id = CAST(p.id AS UUID));
