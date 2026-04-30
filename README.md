# Impofer E-Commerce Platform

Backend and static frontend for an industrial hardware and supplies store, built with Spring Boot, PostgreSQL, JWT-based authentication, and administrative modules for catalog, orders, customers, quotations, and claims management.

## Overview

This repository contains a production-oriented commerce application with:

- Spring Boot 4 and Java 17
- PostgreSQL with Flyway migrations
- JWT authentication and role-based access control
- Static storefront and admin interfaces served by the application
- Email support for account recovery flows
- Docker and reverse proxy deployment assets

## Main Capabilities

- Product catalog and category browsing
- Customer registration and login
- Order management and order history
- Administrative dashboards for inventory, clients, orders, quotations, and claims
- Health and monitoring endpoints through Spring Boot Actuator

## Technology Stack

- Java 17
- Spring Boot 4.0.2
- Spring Web, Spring Security, Spring Data JPA, Spring Validation
- PostgreSQL
- Flyway
- Maven
- Docker and Nginx

## Local Run

1. Copy `.env.example` to `.env` and fill in your own values.
2. Ensure PostgreSQL is available and the target database exists.
3. Start the application:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The application is exposed by default at `http://localhost:8080`.

## Security Notes

- No runtime secrets should be committed to this repository.
- Use environment variables or a secrets manager for database, JWT, SMTP, and payment credentials.
- Example values in `.env.example` are placeholders only.

## Deployment

The repository includes deployment artifacts for container-based environments:

- `Dockerfile`
- `docker-compose.yml`
- `nginx/nginx.conf`

Review the deployment guides before publishing to production and inject secrets at deploy time.

## Repository Description

Suggested GitHub description:

`Spring Boot e-commerce platform for industrial supplies with JWT security, PostgreSQL, Flyway migrations, admin modules, and Docker deployment support.`