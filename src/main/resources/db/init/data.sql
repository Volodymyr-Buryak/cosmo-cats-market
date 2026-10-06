MERGE INTO products (id, name, description, price, currency, stock, category_id_ref) KEY (id)
VALUES
    (
        '10000000-0000-4000-8000-000000000001',
        'Galaxy Milk',
        'Lactose-free milk collected from the Andromeda dairy station.',
        12.99,
        'USD',
        120,
        '20000000-0000-4000-8000-000000000001'
    ),
    (
        '10000000-0000-4000-8000-000000000002',
        'Comet Catnip',
        'Freeze-dried catnip grown in nutrient-rich comet dust.',
        8.49,
        'USD',
        85,
        '20000000-0000-4000-8000-000000000001'
    ),
    (
        '10000000-0000-4000-8000-000000000003',
        'Star Tuna Rations',
        'Protein-rich tuna portions packed for long interstellar voyages.',
        24.50,
        'USD',
        64,
        '20000000-0000-4000-8000-000000000001'
    ),
    (
        '10000000-0000-4000-8000-000000000004',
        'Star Yarn',
        'A floating ball of luminous yarn with an anti-tangle field.',
        15.75,
        'USD',
        42,
        '20000000-0000-4000-8000-000000000002'
    ),
    (
        '10000000-0000-4000-8000-000000000005',
        'Comet Tail Teaser',
        'A durable teaser wand that leaves a sparkling comet trail.',
        18.90,
        'USD',
        37,
        '20000000-0000-4000-8000-000000000002'
    ),
    (
        '10000000-0000-4000-8000-000000000006',
        'Galaxy Laser Pointer',
        'A five-pattern laser toy certified for zero-gravity play.',
        29.99,
        'USD',
        58,
        '20000000-0000-4000-8000-000000000002'
    ),
    (
        '10000000-0000-4000-8000-000000000007',
        'Galaxy Gravity Boots',
        'Magnetic paw boots for safe walking inside low-gravity stations.',
        149.00,
        'USD',
        16,
        '20000000-0000-4000-8000-000000000003'
    ),
    (
        '10000000-0000-4000-8000-000000000008',
        'Star Navigator Collar',
        'A smart collar with location beacon and constellation navigation.',
        89.95,
        'USD',
        23,
        '20000000-0000-4000-8000-000000000003'
    ),
    (
        '10000000-0000-4000-8000-000000000009',
        'Comet Shield Carrier',
        'A radiation-shielded travel carrier with panoramic windows.',
        219.00,
        'USD',
        9,
        '20000000-0000-4000-8000-000000000003'
    ),
    (
        '10000000-0000-4000-8000-000000000010',
        'Star Dome Bed',
        'A heated sleeping dome that simulates a calm night sky.',
        74.25,
        'USD',
        31,
        '20000000-0000-4000-8000-000000000004'
    ),
    (
        '10000000-0000-4000-8000-000000000011',
        'Galaxy Cloud Blanket',
        'An ultra-soft thermal blanket woven for cold cargo decks.',
        39.80,
        'USD',
        47,
        '20000000-0000-4000-8000-000000000004'
    ),
    (
        '10000000-0000-4000-8000-000000000012',
        'Comet Dust Litter',
        'Low-dust mineral litter with high absorption in artificial gravity.',
        21.40,
        'USD',
        76,
        '20000000-0000-4000-8000-000000000004'
    );
