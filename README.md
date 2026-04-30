<div align="center">

# 🛒 Impofer — Plataforma de Comercio Electrónico

**Backend y frontend estático para una tienda de ferretería industrial y suministros**

[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org)
[![Docker](https://img.shields.io/badge/Docker-Deployment-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com)
[![JWT](https://img.shields.io/badge/JWT-Security-black?style=for-the-badge&logo=jsonwebtokens)](https://jwt.io)

</div>

---

## 📋 Descripción General

Este repositorio contiene una aplicación de comercio electrónico orientada a producción para la venta de suministros industriales. Incluye tanto el backend REST como las interfaces de usuario estáticas del catálogo, panel administrativo y proceso de compra.

**Características principales del proyecto:**

- 🚀 **Spring Boot 4** con **Java 17**
- 🗄️ **PostgreSQL** con migraciones gestionadas por **Flyway**
- 🔐 **Autenticación JWT** y control de acceso basado en roles
- 🌐 Tienda en línea y panel de administración servidos por la aplicación
- 📧 Soporte de correo electrónico para recuperación de contraseñas
- 🐳 Archivos de despliegue con **Docker** y proxy inverso **Nginx**

---

## ⚡ Funcionalidades Principales

| Módulo | Descripción |
|--------|-------------|
| 🛍️ **Catálogo** | Navegación de productos por categorías |
| 👤 **Clientes** | Registro, inicio de sesión y perfil de usuario |
| 📦 **Pedidos** | Gestión de órdenes e historial de compras |
| 🗂️ **Inventario** | Panel administrativo de productos y stock |
| 💬 **Cotizaciones** | Solicitud y gestión de cotizaciones |
| 📝 **Reclamaciones** | Registro y seguimiento de reclamos |
| 📊 **Monitoreo** | Endpoints de salud vía Spring Boot Actuator |

---

## 🛠️ Stack Tecnológico

<div align="center">

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 17 | Lenguaje principal |
| Spring Boot | 4.0.2 | Framework backend |
| Spring Security | — | Seguridad y autenticación |
| Spring Data JPA | — | Acceso a datos |
| PostgreSQL | — | Base de datos relacional |
| Flyway | — | Migraciones de base de datos |
| Maven | — | Gestor de dependencias |
| Docker + Nginx | — | Contenedorización y proxy inverso |

</div>

---

## 🚀 Ejecución Local

### Requisitos previos
- Java 17+
- PostgreSQL en ejecución con la base de datos creada
- Maven (o usar el wrapper incluido)

### Pasos

**1. Configurar variables de entorno**
```bash
cp .env.example .env
# Editar .env con tus propios valores
```

**2. Iniciar la aplicación**

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

**3. Acceder a la aplicación**

```
http://localhost:8080
```

---

## 🐳 Despliegue con Docker

El repositorio incluye todos los artefactos necesarios para despliegue en contenedores:

```
📁 Raíz del proyecto
├── 🐳 Dockerfile
├── 🐳 docker-compose.yml
└── 📁 nginx/
    └── nginx.conf
```

```bash
# Construir y levantar los servicios
docker-compose up --build
```

> Revisar las guías de despliegue incluidas antes de publicar en producción e inyectar los secretos en tiempo de ejecución.

---

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── config/        # Configuración de seguridad y CORS
│   │   ├── controller/    # Controladores REST
│   │   ├── dto/           # Objetos de transferencia de datos
│   │   ├── model/         # Entidades JPA
│   │   ├── repository/    # Repositorios Spring Data
│   │   ├── security/      # Proveedor JWT y filtros
│   │   └── service/       # Lógica de negocio
│   └── resources/
│       ├── static/        # Frontend HTML/CSS/JS
│       └── db/migration/  # Scripts Flyway (V1–V9)
└── test/                  # Pruebas unitarias e integración
```

---

<div align="center">

Desarrollado con ❤️ · **Impofer** · 2025

</div>