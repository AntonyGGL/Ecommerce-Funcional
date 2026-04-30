#!/bin/bash

# ============================================================================
# SCRIPT DE LIMPIEZA EXHAUSTIVA DE VPS - HOSTINGER
# Esto dejará el VPS como "minuto cero" con solo Ubuntu 22.04 limpio
# ============================================================================
# 
# ADVERTENCIA: Este script es DESTRUCTIVO. Eliminará:
# ❌ Todos los contenedores Docker
# ❌ Todos los volúmenes (base de datos, certificados, etc.)
# ❌ Todas las imágenes Docker
# ❌ El proyecto completo
# ❌ Docker, Git y herramientas instaladas
# ❌ Usuarios no-root creados
# ❌ Certificados SSL
# ❌ Logs del sistema
#
# INSTRUCCIONES:
# 1. Conecta a tu VPS: ssh root@<TU_IP_DEL_VPS>
# 2. Copia toda esta sección y pega en el terminal
# 3. Deja que se ejecute (puede tomar 5-10 minutos)
# ============================================================================

set -e  # Salir si hay algún error

echo "=========================================="
echo "🔴 INICIANDO LIMPIEZA EXHAUSTIVA DEL VPS"
echo "=========================================="
echo ""

# ────────────────────────────────────────────────────────────────────────────
# 1. DETENER Y ELIMINAR CONTENEDORES DOCKER
# ────────────────────────────────────────────────────────────────────────────
echo "📦 Paso 1: Deteniendo contenedores Docker..."
docker compose down -v --remove-orphans 2>/dev/null || true
docker kill $(docker ps -q) 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 2. ELIMINAR TODAS LAS IMÁGENES DOCKER
# ────────────────────────────────────────────────────────────────────────────
echo "🗑️  Paso 2: Eliminando todas las imágenes Docker..."
docker rmi -f $(docker images -aq) 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 3. LIMPIAR VOLÚMENES Y OBJETOS HUÉRFANOS
# ────────────────────────────────────────────────────────────────────────────
echo "💾 Paso 3: Eliminando volúmenes y objetos huérfanos..."
docker system prune -af --volumes 2>/dev/null || true
docker volume prune -f 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 4. DESINSTALAR DOCKER COMPLETAMENTE
# ────────────────────────────────────────────────────────────────────────────
echo "🐳 Paso 4: Desinstalando Docker..."
systemctl stop docker 2>/dev/null || true
systemctl disable docker 2>/dev/null || true
apt-get remove -y docker.io docker-compose-plugin docker-ce docker-ce-cli containerd.io 2>/dev/null || true
apt-get purge -y docker.io docker-compose-plugin 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 5. DESINSTALAR GIT
# ────────────────────────────────────────────────────────────────────────────
echo "🌳 Paso 5: Desinstalando Git..."
apt-get remove -y git git-man 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 6. ELIMINAR USUARIO NO-ROOT
# ────────────────────────────────────────────────────────────────────────────
echo "👤 Paso 6: Eliminando usuario 'impofer'..."
userdel -r impofer 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 7. ELIMINAR CARPETAS DEL PROYECTO Y HOME
# ────────────────────────────────────────────────────────────────────────────
echo "📁 Paso 7: Eliminando proyecto y carpetas de usuario..."
rm -rf /home/* 2>/dev/null || true
rm -rf /root/app 2>/dev/null || true
rm -rf /root/.docker 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 8. LIMPIAR LOGS DEL SISTEMA
# ────────────────────────────────────────────────────────────────────────────
echo "📋 Paso 8: Limpiando logs del sistema..."
truncate -s 0 /var/log/*.log 2>/dev/null || true
rm -rf /var/log/docker 2>/dev/null || true
rm -rf /var/log/apt 2>/dev/null || true
journalctl --vacuum=time=1d 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 9. LIMPIAR ARCHIVOS TEMPORALES
# ────────────────────────────────────────────────────────────────────────────
echo "🗑️  Paso 9: Limpiando archivos temporales..."
rm -rf /tmp/* 2>/dev/null || true
rm -rf /var/tmp/* 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 10. APT CLEANUP
# ────────────────────────────────────────────────────────────────────────────
echo "📦 Paso 10: Limpieza de APT..."
apt-get autoremove -y 2>/dev/null || true
apt-get autoclean -y 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 11. RESETEAR FIREWALL (OPCIONAL - comentado por defecto)
# ────────────────────────────────────────────────────────────────────────────
# echo "🔥 Paso 11: Reseteando firewall..."
# ufw reset -y 2>/dev/null || true

# ────────────────────────────────────────────────────────────────────────────
# 12. RESUMEN FINAL
# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "=========================================="
echo "✅ LIMPIEZA COMPLETADA"
echo "=========================================="
echo ""
echo "Tu VPS está limpio. El espacio disponible es:"
df -h /
echo ""
echo "PRÓXIMOS PASOS:"
echo "1. Actualizar el sistema:"
echo "   apt update && apt upgrade -y"
echo ""
echo "2. Volver a instalar Docker:"
echo "   curl -fsSL https://get.docker.com | sh"
echo "   systemctl enable docker && systemctl start docker"
echo ""
echo "3. Instalar Docker Compose:"
echo "   apt install -y docker-compose-plugin"
echo ""
echo "4. Instalar Git:"
echo "   apt install -y git"
echo ""
echo "5. Subir tu proyecto nuevamente"
echo "=========================================="
