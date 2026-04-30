#!/bin/bash

# ============================================================================
# SCRIPT DE REINSTALACIÓN AUTOMÁTICA DEL PROYECTO EN VPS
# Después de ejecutar el script de limpieza
# ============================================================================
#
# REQUISITOS PREVIOS:
# ✓ Haber ejecutado CLEANUP_VPS_SCRIPT.sh
# ✓ Tener un repositorio Git con tu código (o SCP disponible)
# ✓ Tener tu archivo .env con todas las variables configuradas
#
# INSTRUCCIONES:
# 1. Edita las variables de abajo con TUS valores
# 2. Conecta a tu VPS: ssh root@<TU_IP_DEL_VPS>
# 3. Copia este script completo y pega en el terminal
# 4. Déjalo ejecutar (puede tomar 10-20 minutos)
# ============================================================================

set -e  # Salir si hay error

# ────────────────────────────────────────────────────────────────────────────
# CONFIGURACIÓN - EDITA ESTO CON TUS VALORES
# ────────────────────────────────────────────────────────────────────────────

GITHUB_REPO="https://github.com/tu-usuario/tu-repo.git"  # 👈 Edita esto
APP_USERNAME="impofer"
APP_GROUP="impofer"
APP_HOME="/home/impofer"
APP_DIR="${APP_HOME}/app"
DOMAIN="impofer.com"  # 👈 Edita esto
EMAIL="admin@impofer.com"  # 👈 Edita esto

echo "=========================================="
echo "🚀 INICIANDO REINSTALACIÓN DEL PROYECTO"
echo "=========================================="
echo ""

# ────────────────────────────────────────────────────────────────────────────
# 1. ACTUALIZAR UBUNTU
# ────────────────────────────────────────────────────────────────────────────
echo "📦 Paso 1: Actualizando sistema..."
apt update && apt upgrade -y

# ────────────────────────────────────────────────────────────────────────────
# 2. INSTALAR DOCKER
# ────────────────────────────────────────────────────────────────────────────
echo "🐳 Paso 2: Instalando Docker..."
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# ────────────────────────────────────────────────────────────────────────────
# 3. INSTALAR DOCKER COMPOSE
# ────────────────────────────────────────────────────────────────────────────
echo "📦 Paso 3: Instalando Docker Compose..."
apt install -y docker-compose-plugin
docker compose version

# ────────────────────────────────────────────────────────────────────────────
# 4. INSTALAR GIT
# ────────────────────────────────────────────────────────────────────────────
echo "🌳 Paso 4: Instalando Git..."
apt install -y git

# ────────────────────────────────────────────────────────────────────────────
# 5. CREAR USUARIO NO-ROOT
# ────────────────────────────────────────────────────────────────────────────
echo "👤 Paso 5: Creando usuario '${APP_USERNAME}'..."
useradd -m -d "${APP_HOME}" -s /bin/bash "${APP_USERNAME}" || true
usermod -aG docker "${APP_USERNAME}"

# ────────────────────────────────────────────────────────────────────────────
# 6. CLONAR REPOSITORIO
# ────────────────────────────────────────────────────────────────────────────
echo "📥 Paso 6: Clonando repositorio..."
rm -rf "${APP_DIR}"
git clone "${GITHUB_REPO}" "${APP_DIR}"
cd "${APP_DIR}"
chown -R "${APP_USERNAME}:${APP_GROUP}" "${APP_DIR}"

# ────────────────────────────────────────────────────────────────────────────
# 7. CONFIGURAR VARIABLES DE ENTORNO
# ────────────────────────────────────────────────────────────────────────────
echo "⚙️  Paso 7: Configurando variables de entorno..."
echo ""
echo "⚠️  IMPORTANTE: Necesitas crear el archivo .env"
echo ""
echo "Ejecuta esto como usuario ${APP_USERNAME}:"
echo "  su - ${APP_USERNAME}"
echo "  cd ${APP_DIR}"
echo "  cp .env.example .env"
echo "  nano .env"
echo ""
echo "Rellena OBLIGATORIAMENTE:"
echo "  - DB_USERNAME"
echo "  - DB_PASSWORD"
echo "  - JWT_SECRET (genera con: openssl rand -base64 64)"
echo "  - STRIPE_API_KEY (sk_live_...)"
echo "  - STRIPE_WEBHOOK_SECRET (whsec_...)"
echo "  - MAIL_USERNAME"
echo "  - MAIL_PASSWORD"
echo "  - ALLOWED_ORIGINS (https://${DOMAIN},https://www.${DOMAIN})"
echo "  - APP_BASE_URL (https://${DOMAIN})"
echo "  - NGINX_SERVER_NAME (${DOMAIN})"
echo ""
read -p "👉 Presiona ENTER cuando hayas completado el archivo .env"

# ────────────────────────────────────────────────────────────────────────────
# 8. OBTENER CERTIFICADO SSL
# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "🔒 Paso 8: Obteniendo certificado SSL..."
cd "${APP_DIR}"

# Iniciar solo Nginx
docker compose up -d nginx
sleep 5

# Obtener certificado
docker run --rm \
  -v certbot_conf:/etc/letsencrypt \
  -v certbot_www:/var/www/certbot \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "${EMAIL}" \
    --agree-tos \
    --no-eff-email \
    -d "${DOMAIN}" \
    -d "www.${DOMAIN}" \
    --non-interactive

echo "✅ Certificado obtenido"

# ────────────────────────────────────────────────────────────────────────────
# 9. LEVANTAR TODO
# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "🚀 Paso 9: Levantando todos los servicios..."
docker compose down
docker compose up -d --build

# Esperar a que se inicie la app
sleep 10

# ────────────────────────────────────────────────────────────────────────────
# 10. VERIFICAR ESTADO
# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "📊 Paso 10: Verificando estado..."
docker compose ps
echo ""

# ────────────────────────────────────────────────────────────────────────────
# 11. FIREWALL
# ────────────────────────────────────────────────────────────────────────────
echo "🔥 Paso 11: Configurando firewall..."
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable -y
ufw status

# ────────────────────────────────────────────────────────────────────────────
# RESUMEN FINAL
# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "=========================================="
echo "✅ INSTALACIÓN COMPLETADA"
echo "=========================================="
echo ""
echo "📍 URLs:"
echo "  Tienda:      https://${DOMAIN}"
echo "  Login:       https://${DOMAIN}/login.html"
echo "  Admin:       https://${DOMAIN}/admin.html"
echo "  Health:      https://${DOMAIN}/actuator/health"
echo ""
echo "📋 Comandos útiles:"
echo "  Ver logs:         docker compose logs -f"
echo "  Reiniciar app:    docker compose restart app"
echo "  Detener:          docker compose down"
echo "  Ver estado:       docker compose ps"
echo ""
echo "🔐 Certificado SSL:"
echo "  Se renueva automáticamente cada 12 horas"
echo "  Ubicado en:       /etc/letsencrypt/live/${DOMAIN}"
echo ""
echo "=========================================="
