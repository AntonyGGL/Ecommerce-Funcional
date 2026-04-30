#!/bin/bash
# =============================================================
# Script de deployment en el VPS — se ejecuta en el servidor
# =============================================================
set -e

APP_DIR="/root/app"

echo ""
echo "======================================================"
echo "  DEPLOY IMPOFER — $(date)"
echo "======================================================"

# 1. Parar y eliminar versión anterior
echo ""
echo "[1/5] Deteniendo version anterior..."
cd "$APP_DIR" 2>/dev/null && docker compose down --remove-orphans 2>/dev/null || true

# 2. Eliminar imágenes viejas de la app (no DB ni Nginx)
echo "[2/5] Limpiando imagenes antiguas..."
docker image rm app-app 2>/dev/null || true
docker image rm impofer-app 2>/dev/null || true
docker image prune -f 2>/dev/null || true

# 3. Ir al directorio y construir
echo "[3/5] Construyendo imagen Docker (esto tarda unos minutos)..."
cd "$APP_DIR"
docker compose build --no-cache app

# 4. Levantar todos los servicios
echo "[4/5] Levantando servicios..."
docker compose up -d

# 5. Esperar y verificar
echo "[5/5] Esperando a que la app arranque (60s)..."
sleep 60

echo ""
echo "=== ESTADO DE CONTAINERS ==="
docker compose ps

echo ""
echo "=== HEALTH CHECK ==="
curl -s http://localhost:8080/actuator/health 2>/dev/null || curl -s http://localhost/actuator/health 2>/dev/null || echo "Aun arrancando..."

echo ""
echo "======================================================"
echo "  DEPLOYMENT COMPLETADO"
echo "======================================================"
