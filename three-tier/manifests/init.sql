CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    price       DOUBLE PRECISION NOT NULL,
    stock       INTEGER DEFAULT 0
);
INSERT INTO products (name, description, price, stock) VALUES
    ('Laptop Pro 15',     'Laptop de alto rendimiento con 32GB RAM',      1299.99, 15),
    ('Monitor UltraWide', 'Monitor 34" curvo 144Hz',                       699.99, 30),
    ('Teclado Mecánico',  'Teclado mecánico con switches Cherry MX Red',    89.99, 50),
    ('Mouse Ergonómico',  'Mouse inalámbrico con 6 botones programables',   49.99, 80),
    ('Webcam 4K',         'Cámara web con enfoque automático y micrófono', 129.99, 25),
    ('Hub USB-C',         'Hub 7 puertos con carga rápida PD 100W',         59.99, 40);
