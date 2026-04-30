# Guía de Inicio - Aplicación Impofer

## Requisitos Previos

- **Java 17+**: Instalar desde [adoptopenjdk.net](https://adoptopenjdk.net/)
- **Maven 3.8+**: Instalar desde [maven.apache.org](https://maven.apache.org/)
- **PostgreSQL 12+**: Instalar desde [postgresql.org](https://www.postgresql.org/)

## Configuración de Base de Datos

1. **Crear la base de datos** (en la terminal o pgAdmin):
```sql
CREATE DATABASE impofer_db ENCODING UTF8;
```

2. **Verificar conexión** a PostgreSQL con usuario `postgres`:
```bash
psql -U postgres -d impofer_db -c "SELECT version();"
```

## Ejecutar la Aplicación

### Opción 1: Usando Maven desde el terminal

1. **Navegar al directorio del proyecto**:
```bash
cd c:\Proyectos\demo
```

2. **Compilar el proyecto**:
```bash
mvn clean compile
```

3. **Ejecutar la aplicación**:
```bash
mvn spring-boot:run
```

La aplicación se iniciará en: `http://localhost:8080`

### Opción 2: Usando VS Code

1. Abrir el proyecto en VS Code
2. Ejecutar la tarea: `Ctrl + Shift + B` → Seleccionar "maven: clean spring-boot:run"
3. La aplicación se iniciará en el puerto 8080

## Acceso a la Aplicación

### Usuarios de Prueba

**Admin:**
- Email: `admin@impofer.com`
- Contraseña: `admin123`

**Cliente:**
- Email: `cliente@example.com`
- Contraseña: `cliente123`

### Rutas Disponibles

| Ruta | Descripción |
|------|-------------|
| `/` | Página de inicio |
| `/catalog.html` | Catálogo de productos |
| `/login.html` | Formulario de inicio de sesión |
| `/checkout.html` | Carrito de compras |
| `/admin.html` | Panel administrativo |

## Endpoints de API Disponibles

### Autenticación
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrarse
- `POST /api/auth/validate` - Validar token JWT

### Productos
- `GET /api/products` - Listar todos los productos
- `GET /api/products/featured` - Productos destacados (máx 4)
- `GET /api/products/{id}` - Obtener producto por ID
- `GET /api/products/category/{categoryId}` - Productos por categoría
- `POST /api/products` - Crear producto (ADMIN)
- `PUT /api/products/{id}` - Actualizar producto (ADMIN)
- `DELETE /api/products/{id}` - Eliminar producto (ADMIN)

### Órdenes
- `GET /api/orders` - Listar órdenes
- `GET /api/orders/{id}` - Obtener orden por ID
- `GET /api/orders/user/{userId}` - Órdenes del usuario
- `POST /api/orders` - Crear nueva orden
- `PUT /api/orders/{id}/status` - Actualizar estado de orden (ADMIN)
- `DELETE /api/orders/{id}` - Eliminar orden (ADMIN)

### Clientes
- `GET /api/clients` - Listar clientes (ADMIN)
- `GET /api/clients/{id}` - Obtener cliente (ADMIN)
- `POST /api/clients` - Crear cliente (ADMIN)
- `PUT /api/clients/{id}` - Actualizar cliente (ADMIN)
- `DELETE /api/clients/{id}` - Eliminar cliente (ADMIN)

### Administración
- `GET /api/admin/stats` - Estadísticas del sistema

## Autenticación con JWT

La aplicación utiliza JWT (JSON Web Tokens) para autenticación:

1. **Obtener token**: Hacer login en `/api/auth/login`
2. **Usar token**: Incluir en header: `Authorization: Bearer {token}`
3. **Token válido por**: 24 horas

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── controller/      # Controladores REST
│   │   ├── service/         # Lógica de negocio
│   │   ├── model/           # Entidades JPA
│   │   ├── repository/      # Interfaces de datos
│   │   ├── config/          # Configuración (seguridad, BD)
│   │   ├── security/        # Componentes JWT
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Manejo de excepciones
│   │   └── DemoApplication.java
│   └── resources/
│       ├── static/          # HTML, CSS, JS
│       ├── application.properties
│       └── init-db.sql
└── test/
```

## Solución de Problemas

### Error: "Connection refused" (Base de datos)
- Verificar que PostgreSQL esté ejecutándose
- Verificar credenciales en `application.properties`
- Crear base de datos: `CREATE DATABASE impofer_db;`

### Error: "Port 8080 already in use"
- Cambiar puerto en `application.properties`: `server.port=8081`

### Error: "Build failure"
- Limpiar caché: `mvn clean`
- Reinstalar dependencias: `mvn dependency:resolve`

## Características de la Aplicación

✅ **Autenticación JWT** con roles (ADMIN, CUSTOMER, VENDOR)
✅ **Gestión de Productos** con categorías
✅ **Carrito de Compras** en localStorage
✅ **Sistema de Órdenes** con estados múltiples
✅ **Panel Administrativo** con estadísticas
✅ **Validación de Datos** con anotaciones @Valid
✅ **Manejo de Errores** global con @RestControllerAdvice
✅ **CORS** habilitado para acceso desde frontend

## Próximas Mejoras Planeadas

- [ ] Integración con pasarela de pago
- [ ] Envíos con rastreo
- [ ] Notificaciones por email
- [ ] Documentación API con Swagger/OpenAPI
- [ ] Tests unitarios e integración
- [ ] Deployment en Docker/Kubernetes
