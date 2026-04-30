#!/bin/bash

# Script para crear la base de datos PostgreSQL para Impofer
# Ejecutar este script con: bash setup-database.sh

echo "Creando base de datos PostgreSQL para Impofer..."

# Verificar si postgresql está instalado
if ! command -v psql &> /dev/null; then
    echo "PostgreSQL no está instalado. Por favor, instálalo primero."
    exit 1
fi

# Crear la base de datos
echo "Conectando a PostgreSQL como usuario 'postgres'..."
psql -U postgres -c "CREATE DATABASE impofer_db ENCODING 'UTF8';" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Base de datos 'impofer_db' creada exitosamente"
else
    echo "✗ Error al crear la base de datos (puede que ya exista)"
fi

echo ""
echo "Verificando la conexión a la base de datos..."
psql -U postgres -d impofer_db -c "SELECT 'Conexión exitosa' as status;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Conexión a la base de datos verificada"
else
    echo "✗ Error en la conexión. Verifique las credenciales de PostgreSQL."
    exit 1
fi

echo ""
echo "Base de datos lista. Ejecuta: mvn spring-boot:run"
