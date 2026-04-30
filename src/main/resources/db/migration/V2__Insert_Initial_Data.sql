-- Insert categories
INSERT INTO categories (name, description) VALUES 
('MÁQUINAS', 'Equipos pesados e industriales'),
('REPUESTOS', 'Componentes y partes originales'),
('ACCESORIOS', 'Complementos para herramientas'),
('OFERTAS', 'Productos con descuentos especiales'),
('CONSTRUCCIÓN', 'Materiales y herramientas de obra');

-- Insert initial products
INSERT INTO products (name, description, price, stock, min_stock, sku, image_url, rating, category_id, active) VALUES 
('Motobomba Industrial 7HP', 'Alta presión para riego', 450.00, 10, 5, 10000, 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?auto=format&fit=crop&q=80&w=200', 4.8, 1, true),
('Kit Carburador GX160', 'Repuesto original Honda', 35.50, 25, 5, 10001, 'https://images.unsplash.com/photo-1625047509168-a704706927d6?auto=format&fit=crop&q=80&w=200', 4.5, 2, true),
('Disco Diamante 9"', 'Corte granito y concreto', 12.90, 100, 10, 10002, 'https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&q=80&w=200', 4.9, 3, true),
('Soldadora Inverter 200A', 'Oferta exclusiva por tiempo limitado', 120.00, 15, 5, 10003, 'https://images.unsplash.com/photo-1534346738543-bc0002d24599?auto=format&fit=crop&q=80&w=200', 4.7, 4, true),
('Nivel Laser Autonivelante', 'Precisión profesional para interiores', 85.00, 20, 5, 10004, 'https://images.unsplash.com/photo-1617103996702-96ff29b1c467?auto=format&fit=crop&q=80&w=200', 5.0, 5, true);

-- Insert admin user (password: admin123 encoded)
INSERT INTO users (email, password, first_name, last_name, company, phone, address, role, active) VALUES 
('admin@impofer.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00dmxs.TVuHOn2', 'Admin', 'Impofer', 'Impofer Inc', '+51999000000', 'Lima, Perú', 'ADMIN', true);

-- Insert example client (password: cliente123 encoded)
INSERT INTO users (email, password, first_name, last_name, company, phone, address, role, active) VALUES 
('cliente@example.com', '$2a$10$wEIDm20W5V.8H1/9Ld/OfuS.N7J7V9wX4H3v3v3v3v3v3v3v3v3v', 'Carlos', 'Mendoza', 'Constructora Axis', '+51999123456', 'Av. Industrial 405', 'CUSTOMER', true);
