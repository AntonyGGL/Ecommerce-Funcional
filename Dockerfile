# ============================================================
# Etapa 1: Build — compila el proyecto con Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias primero (cache de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ============================================================
# Etapa 2: Runtime — imagen mínima solo con el JAR
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios para mayor seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar solo el JAR desde la etapa de build
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Puerto expuesto (debe coincidir con PORT en las variables de entorno)
EXPOSE 8080

# Arrancar la app con perfil de produccion
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
