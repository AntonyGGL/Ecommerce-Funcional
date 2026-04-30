-- Libro de Reclamaciones (INDECOPI)
CREATE TABLE reclamaciones (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('QUEJA', 'RECLAMO')),
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    nombre_consumidor VARCHAR(255) NOT NULL,
    domicilio VARCHAR(500),
    correo VARCHAR(255) NOT NULL,
    telefono VARCHAR(20),
    bien_contratado TEXT NOT NULL,
    monto_reclamado DECIMAL(19, 2),
    descripcion TEXT NOT NULL,
    pedido TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
