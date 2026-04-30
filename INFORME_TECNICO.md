# Informe Técnico — Plataforma SaaS Tiendas Inteligentes
**Fecha:** Febrero 2026  
**Estado del proyecto:** En desarrollo activo  
**Stack principal:** Java 17 · Spring Boot 4.0 · PostgreSQL 16 · Docker

---

## 1. Análisis MVP — SaaS Tiendas Inteligentes

### Objetivo del MVP
Plataforma de comercio electrónico B2B orientada a la venta de maquinaria industrial, repuestos y accesorios. Permite a clientes empresariales realizar pedidos en línea con gestión de inventario en tiempo real y procesamiento de pagos integrado.

### Funcionalidades del MVP definidas

| Módulo | Funcionalidad | Estado |
|---|---|---|
| Autenticación | Registro, login, recuperación de contraseña, JWT | Implementado |
| Catálogo | Listado de productos por categoría, búsqueda, filtros | Implementado |
| Carrito | Agregar/quitar productos, cálculo de totales | Implementado |
| Pedidos | Creación, seguimiento de estado, historial | Implementado |
| Pagos | Integración pasarela de pagos con webhooks | Implementado |
| Administración | CRUD productos, gestión de clientes, panel de pedidos | Implementado |
| Email | Notificaciones automáticas de pedidos y contraseña | Implementado |
| Inventario | Control de stock mínimo, alertas | Implementado |

### Roles de usuario
- **ADMIN** — Gestión completa del sistema (productos, clientes, pedidos, inventario)
- **CUSTOMER** — Navegación de catálogo, carrito, checkout, historial de órdenes

---

## 2. Backlog Técnico — Implementación SaaS

### Épicas identificadas

**Épica 1 — Autenticación y Seguridad**
- Registro y login con JWT
- Refresh de tokens
- Recuperación de contraseña por email (token de reset)
- Roles y permisos por endpoint

**Épica 2 — Catálogo y Productos**
- CRUD de productos y categorías (admin)
- Visualización pública del catálogo con filtros
- Control de stock y alertas de inventario mínimo
- Gestión de imágenes por URL

**Épica 3 — Carrito y Checkout**
- Carrito persistente por usuario autenticado
- Cálculo de subtotal, impuestos y envío
- Validación de stock en tiempo real al agregar al carrito

**Épica 4 — Pedidos**
- Creación de orden desde carrito
- Estados de orden: `PENDING → PROCESSING → SHIPPED → DELIVERED`
- Estados de pago: `PENDING → PAID → REFUNDED`
- Estados de envío independientes del pago
- Historial de pedidos por cliente

**Épica 5 — Pagos**
- Integración con pasarela de pagos externa
- Manejo de webhooks para confirmación asíncrona
- Almacenamiento de `payment_intent_id` por orden

**Épica 6 — Administración**
- Panel de administración de clientes (activar/desactivar)
- Panel de inventario con alertas de stock mínimo
- Panel de pedidos con cambio de estado
- Dashboard con métricas básicas

**Épica 7 — Infraestructura y DevOps**
- Contenedorización con Docker + Docker Compose
- Migraciones de base de datos con Flyway
- Perfiles separados: desarrollo (`default`) y producción (`prod`)
- Variables de entorno para toda configuración sensible

---

## 3. Diseño de Base de Datos Físico

### Diagrama de entidades

```
users ──────────────────┐
  id (PK)               │
  email (UNIQUE)        │
  password (hash)       │ 1
  first_name            │
  last_name             │
  company               │
  phone                 │
  address               │
  role                  │
  active                │
  reset_token           │ N
  created_at            ▼
  updated_at      orders
                    id (PK)
categories          user_id (FK → users)
  id (PK)           total
  name (UNIQUE)     subtotal
  description       tax
       │            shipping_cost
       │ 1          status
       │            payment_status
       ▼ N          shipping_status
   products         payment_intent_id
     id (PK)        shipping_address
     name           notes
     description    created_at
     price          updated_at
     stock               │
     min_stock           │ 1
     sku (UNIQUE)        │
     image_url           │
     rating              │ N
     category_id (FK)    ▼
     active        order_details
     created_at      id (PK)
     updated_at      order_id (FK → orders)
                     product_id (FK → products)
cart_items           quantity
  id (PK)            unit_price
  user_id (FK)
  product_id (FK)
  quantity
```

### Historial de migraciones (Flyway)

| Versión | Descripción |
|---|---|
| V1 | Esquema inicial: users, categories, products, orders, order_details, cart_items |
| V2 | Datos iniciales de categorías, productos de muestra y usuarios base |
| V3 | Campos para recuperación de contraseña (reset token + expiración) |
| V4 | Campo `payment_intent_id` en tabla orders |

---

## 4. Diseño Técnico de Arquitectura SaaS

### Arquitectura general

```
┌─────────────────────────────────────────────────────┐
│                   CLIENTE (Browser)                  │
│         HTML + CSS + JS (Vanilla, sin framework)     │
└──────────────────────────┬──────────────────────────┘
                           │ HTTPS / REST JSON
┌──────────────────────────▼──────────────────────────┐
│              Spring Boot Application                 │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │ Controllers │  │   Services   │  │ Repositories│  │
│  │  (REST API) │→ │ (Lógica)     │→ │ (JPA/Data) │  │
│  └─────────────┘  └──────────────┘  └─────┬──────┘  │
│  ┌─────────────┐  ┌──────────────┐        │         │
│  │  Security   │  │ Flyway Mig.  │        │         │
│  │  (JWT/BCrypt│  │ (Esquema DB) │        │         │
│  └─────────────┘  └──────────────┘        │         │
└───────────────────────────────────────────┼─────────┘
                                            │
┌───────────────────────────────────────────▼─────────┐
│                  PostgreSQL 16                       │
│              (persitencia principal)                 │
└─────────────────────────────────────────────────────┘

Servicios externos:
  → Pasarela de pagos (webhooks de confirmación)
  → Servicio SMTP (notificaciones por email)
```

### Capas de la aplicación

| Capa | Componente | Responsabilidad |
|---|---|---|
| Presentación | `static/*.html` | Vistas del cliente y admin (sin SSR) |
| API REST | `controller/` | Endpoints HTTP, validación de entrada, respuestas JSON |
| Seguridad | `security/` | Filtros JWT, gestión de sesión sin estado (stateless) |
| Negocio | `service/` | Lógica de dominio, transacciones, reglas de negocio |
| Persistencia | `repository/` | Acceso a datos con Spring Data JPA |
| Modelo | `model/` + `dto/` | Entidades de BD y objetos de transferencia de datos |
| Configuración | `config/` | Beans de configuración (CORS, seguridad, etc.) |

### Patrones aplicados
- **Stateless JWT**: sin sesiones en servidor, token en cada petición
- **DTO pattern**: separación entre entidades de BD y objetos de respuesta API
- **Repository pattern**: abstracción de acceso a datos
- **Profiles de Spring**: configuración diferenciada por entorno (dev/prod)
- **Webhook pattern**: confirmación asíncrona de pagos

---

## 5. Especificación de Tablas de Base de Datos

### `users`
| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| id | BIGSERIAL | PK | Identificador único |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Correo electrónico (login) |
| password | VARCHAR(255) | NOT NULL | Hash BCrypt |
| first_name | VARCHAR(255) | NOT NULL | Nombre |
| last_name | VARCHAR(255) | NOT NULL | Apellido |
| company | VARCHAR(255) | — | Empresa del cliente |
| phone | VARCHAR(255) | — | Teléfono de contacto |
| address | VARCHAR(255) | — | Dirección de envío por defecto |
| role | VARCHAR(50) | NOT NULL, DEFAULT 'CUSTOMER' | ADMIN o CUSTOMER |
| active | BOOLEAN | DEFAULT TRUE | Habilitación de cuenta |
| reset_token | VARCHAR | — | Token temporal de recuperación de contraseña |
| reset_token_expiry | TIMESTAMP | — | Expiración del token de reset |
| created_at | TIMESTAMP | NOT NULL | Fecha de registro |
| updated_at | TIMESTAMP | NOT NULL | Última modificación |

### `categories`
| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| id | BIGSERIAL | PK | Identificador único |
| name | VARCHAR(255) | NOT NULL, UNIQUE | Nombre de categoría |
| description | TEXT | — | Descripción de la categoría |

### `products`
| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| id | BIGSERIAL | PK | Identificador único |
| name | VARCHAR(255) | NOT NULL | Nombre del producto |
| description | TEXT | NOT NULL | Descripción detallada |
| price | DECIMAL(19,2) | NOT NULL | Precio unitario |
| stock | INTEGER | NOT NULL, DEFAULT 0 | Unidades disponibles |
| min_stock | INTEGER | NOT NULL, DEFAULT 5 | Umbral de alerta de inventario |
| sku | BIGINT | NOT NULL, UNIQUE | Código único de producto |
| image_url | VARCHAR(255) | — | URL de imagen del producto |
| rating | DECIMAL(3,2) | DEFAULT 0.0 | Calificación promedio (0–5) |
| category_id | BIGINT | FK → categories | Categoría asignada |
| active | BOOLEAN | DEFAULT TRUE | Visibilidad en catálogo |
| created_at | TIMESTAMP | NOT NULL | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL | Última modificación |

### `orders`
| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| id | BIGSERIAL | PK | Identificador único |
| user_id | BIGINT | FK → users, NOT NULL | Cliente que realizó el pedido |
| total | DECIMAL(19,2) | NOT NULL | Total final del pedido |
| subtotal | DECIMAL(19,2) | NOT NULL | Subtotal sin impuestos ni envío |
| tax | DECIMAL(19,2) | NOT NULL | Impuestos calculados |
| shipping_cost | DECIMAL(19,2) | DEFAULT 0 | Costo de envío |
| status | VARCHAR(50) | DEFAULT 'PENDING' | Estado general del pedido |
| payment_status | VARCHAR(50) | DEFAULT 'PENDING' | Estado del pago |
| shipping_status | VARCHAR(50) | DEFAULT 'PENDING' | Estado del envío |
| payment_intent_id | VARCHAR | — | ID de referencia del pago externo |
| shipping_address | TEXT | — | Dirección de entrega del pedido |
| notes | TEXT | — | Notas adicionales |
| created_at | TIMESTAMP | NOT NULL | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL | Última modificación |

### `order_details`
| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| id | BIGSERIAL | PK | Identificador único |
| order_id | BIGINT | FK → orders | Pedido al que pertenece |
| product_id | BIGINT | FK → products | Producto incluido |
| quantity | INTEGER | NOT NULL | Cantidad comprada |
| unit_price | DECIMAL(19,2) | NOT NULL | Precio unitario al momento de compra |

### `cart_items`
| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| id | BIGSERIAL | PK | Identificador único |
| user_id | BIGINT | FK → users | Propietario del carrito |
| product_id | BIGINT | FK → products | Producto en el carrito |
| quantity | INTEGER | NOT NULL | Cantidad seleccionada |

---

## 6. Plan de Pruebas TDD por Épica

### Estrategia de testing

La suite de pruebas sigue un enfoque de caja blanca (unit + integración) y caja negra (blackbox/end-to-end de controladores).

### Cobertura actual por épica

**Épica 1 — Autenticación**
- `AuthControllerBlackBoxTest` — Flujos de registro, login, token inválido, credenciales incorrectas
- `UserServiceTest` / `UserServiceWhiteBoxTest` — Validaciones de negocio, hashing de contraseña, gestión de roles

**Épica 2 — Catálogo y Productos**
- `ProductControllerBlackBoxTest` — Listado, filtros, acceso sin autenticación al catálogo
- `ProductServiceWhiteBoxTest` — Cálculo de stock, validaciones de precio y SKU duplicado

**Épica 3 — Carrito**
- `CartControllerBlackBoxTest` — Agregar/quitar ítems, stock insuficiente, carrito vacío
- `CartServiceTest` / `CartServiceWhiteBoxTest` — Cálculo de totales, límites de stock

**Épica 4 — Pedidos**
- `OrderControllerBlackBoxTest` — Creación de orden, cambio de estado, acceso por rol
- `OrderServiceTest` / `OrderServiceWhiteBoxTest` — Transiciones de estado, descuento de stock al confirmar

**Épica 5 — Administración**
- `AdminControllerBlackBoxTest` — Acceso restringido por rol ADMIN, CRUD de productos
- `ClientControllerTest` — Gestión de clientes, activación/desactivación

**Integración general**
- `BlackBoxIntegrationTests` — Flujo completo: registro → login → carrito → checkout → orden

### Tipos de pruebas

| Tipo | Herramienta | Alcance |
|---|---|---|
| Unit test | JUnit 5 + Mockito | Servicios aislados de dependencias |
| Caja blanca | JUnit 5 | Ramas internas de lógica de negocio |
| Caja negra | MockMvc / SpringBootTest | Endpoints HTTP completos |
| Integración | SpringBootTest + H2/Testcontainers | Flujos de punta a punta |

---

## 7. Planificación del Proyecto

### Fases de desarrollo

| Fase | Contenido | Estado |
|---|---|---|
| Fase 0 — Infraestructura base | Setup Spring Boot, Docker, Flyway, CI config | Completado |
| Fase 1 — Autenticación | JWT, registro, login, reset password | Completado |
| Fase 2 — Catálogo | Modelos, CRUD productos/categorías, vistas | Completado |
| Fase 3 — Carrito y pedidos | Cart persistente, creación de órdenes, estados | Completado |
| Fase 4 — Pagos | Integración pasarela, webhooks, payment_intent | Completado |
| Fase 5 — Administración | Paneles admin, gestión clientes/inventario | Completado |
| Fase 6 — Testing | Suite completa blackbox + whitebox + integración | En progreso |
| Fase 7 — Productivización | Env vars, Docker Compose prod, optimización | En progreso |

### Dependencias técnicas principales

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 4.0.2 | Framework backend |
| Spring Security | (incluido) | Autenticación y autorización |
| Spring Data JPA | (incluido) | ORM y repositorios |
| PostgreSQL | 16 | Base de datos principal |
| Flyway | (incluido) | Migraciones de esquema |
| JJWT | 0.11.5 | Generación y validación JWT |
| Pasarela de pagos SDK | 24.x | Procesamiento de pagos |
| Spring Mail | (incluido) | Envío de correos transaccionales |
| Docker / Docker Compose | — | Contenedorización y orquestación |
| HikariCP | (incluido) | Pool de conexiones a BD |

---

## 8. Roadmap Semanal de Implementación

### Sprint actual y próximos pasos

| Semana | Objetivos |
|---|---|
| Semana actual | Estabilización de suite de pruebas, corrección de bugs en flujos de pago |
| Semana +1 | Revisión de seguridad (headers, CORS, rate limiting), validación de inputs |
| Semana +2 | Configuración de entorno de staging, pruebas de carga básicas |
| Semana +3 | Preparación del entorno de producción, definición de dominio y SSL |
| Semana +4 | Despliegue a producción, monitoreo inicial, documentación de operaciones |

### Deuda técnica identificada

- Migrar de HTML estático a framework frontend (React/Vue) para mejor mantenibilidad
- Implementar paginación en endpoints de listados (productos, pedidos, clientes)
- Agregar caché en endpoints de catálogo de alta lectura
- Documentación de API con OpenAPI/Swagger
- Pipeline CI/CD automatizado (GitHub Actions o similar)
- Logging estructurado y monitoreo de métricas de aplicación

---

## 9. Tablero Técnico — Estructura de Gestión (Jira/Trello)

### Estructura de tablero recomendada

**Columnas del tablero Kanban:**
```
BACKLOG → REFINADO → EN DESARROLLO → EN REVISIÓN → QA/TESTING → DONE
```

**Etiquetas (labels) por tipo:**
- `feature` — Nueva funcionalidad
- `bug` — Corrección de error
- `tech-debt` — Mejora técnica interna
- `infra` — Infraestructura y DevOps
- `security` — Relacionado con seguridad
- `testing` — Pruebas y cobertura

**Estructura de épicas en tablero:**
```
ÉPICA: Autenticación
  └─ STORY: Login con JWT
  └─ STORY: Recuperación de contraseña
  └─ BUG: Token no expira correctamente

ÉPICA: Catálogo
  └─ STORY: Filtro por categoría
  └─ STORY: Paginación de productos
  └─ TECH-DEBT: Caché en endpoint catálogo

ÉPICA: Carrito y Checkout
  └─ STORY: Validación de stock en tiempo real
  └─ STORY: Cálculo automático de impuestos

ÉPICA: Pedidos
  └─ STORY: Notificación por email al cambiar estado
  └─ STORY: Exportar historial de pedidos

ÉPICA: Pagos
  └─ STORY: Manejo de webhook de reembolso
  └─ BUG: Reintentar webhook fallido

ÉPICA: Administración
  └─ STORY: Dashboard con métricas de ventas
  └─ STORY: Exportar reporte de inventario

ÉPICA: Infraestructura
  └─ INFRA: Pipeline CI/CD
  └─ INFRA: Entorno de staging
  └─ SECURITY: Auditoría de endpoints públicos
```

### Métricas de seguimiento sugeridas

| Métrica | Objetivo |
|---|---|
| Velocidad de sprint | Puntos completados por semana |
| Cobertura de tests | ≥ 80% en clases de servicio |
| Lead time de bugs críticos | < 24 horas en producción |
| Uptime objetivo | ≥ 99.5% mensual |

---

## Resumen Ejecutivo

| Aspecto | Estado |
|---|---|
| Arquitectura backend | Sólida, multicapa, stateless |
| Modelo de datos | Normalizado, con migraciones versionadas |
| Seguridad | JWT implementado, BCrypt para contraseñas, separación de roles |
| Pagos | Integración funcional con confirmación asíncrona via webhook |
| Testing | Suite en construcción, cobertura blackbox + whitebox |
| Despliegue | Docker Compose listo, pendiente entorno de producción |
| Deuda técnica | Frontend estático, sin paginación ni caché, sin CI/CD automatizado |
