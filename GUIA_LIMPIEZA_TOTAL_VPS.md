# 🔄 Guía Completa: Limpiar y Reinstalar el VPS desde Cero

## 📋 Resumen del Proceso

Este proceso limpia el VPS **completamente** y lo reinstala con tu proyecto desde cero.

**Tiempo estimado:** 30-40 minutos (incluyendo certificado SSL)

---

## ⚠️ ADVERTENCIAS CRÍTICAS

- ❌ **Se eliminarán TODOS los datos de la BD**
- ❌ **Se perderán TODOS los certificados SSL actuales**
- ❌ **Se eliminarán TODOS los contenedores y volúmenes Docker**
- ❌ **No se podrá revertir**

**Asegúrate de tener:**
✅ Backup de la BD si necesitas los datos
✅ Tu repositorio Git actualizado
✅ El archivo `.env` con todas las variables configuradas
✅ Acceso SSH al VPS

---

## 🚀 Proceso Paso a Paso

### **PASO 1: Preparar el archivo .env en local**

Antes de todo, en tu PC (`c:\Proyectos\demo\`), crea o actualiza el archivo `.env`:

```bash
# .env (completa con tus valores)
DB_USERNAME=<db_user>
DB_PASSWORD=<db_password>
JWT_SECRET=<jwt_secret_min_64_chars>
JWT_EXPIRATION=14400000
STRIPE_API_KEY=<stripe_secret_key>
STRIPE_WEBHOOK_SECRET=<stripe_webhook_secret>
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<smtp_username>
MAIL_PASSWORD=<smtp_app_password>
ALLOWED_ORIGINS=https://impofer.com,https://www.impofer.com
APP_BASE_URL=https://impofer.com
NGINX_SERVER_NAME=impofer.com
```

**Nota:** Para `JWT_SECRET`, ejecuta en tu PC:
```bash
openssl rand -base64 64
```

---

### **PASO 2: Conectar al VPS por SSH**

Abre PowerShell o terminal y conecta:

```powershell
# En Windows PowerShell
ssh root@<TU_IP_DEL_VPS>

# Ejemplo:
ssh root@165.232.123.45
```

---

### **PASO 3: Ejecutar el Script de Limpieza**

Una vez conectado al VPS, copia TODO este comando y pégalo:

```bash
bash -c 'cat > /tmp/cleanup.sh << "EOF"
#!/bin/bash
set -e
echo "=========================================="
echo "🔴 INICIANDO LIMPIEZA EXHAUSTIVA DEL VPS"
echo "=========================================="
echo ""
echo "📦 Paso 1: Deteniendo contenedores Docker..."
docker compose down -v --remove-orphans 2>/dev/null || true
docker kill $(docker ps -q) 2>/dev/null || true
echo "🗑️  Paso 2: Eliminando todas las imágenes Docker..."
docker rmi -f $(docker images -aq) 2>/dev/null || true
echo "💾 Paso 3: Eliminando volúmenes..."
docker system prune -af --volumes 2>/dev/null || true
docker volume prune -f 2>/dev/null || true
echo "🐳 Paso 4: Desinstalando Docker..."
systemctl stop docker 2>/dev/null || true
systemctl disable docker 2>/dev/null || true
apt-get remove -y docker.io docker-compose-plugin docker-ce docker-ce-cli containerd.io 2>/dev/null || true
apt-get purge -y docker.io docker-compose-plugin 2>/dev/null || true
echo "🌳 Paso 5: Desinstalando Git..."
apt-get remove -y git git-man 2>/dev/null || true
echo "👤 Paso 6: Eliminando usuario 'impofer'..."
userdel -r impofer 2>/dev/null || true
echo "📁 Paso 7: Eliminando carpetas..."
rm -rf /home/* 2>/dev/null || true
rm -rf /root/app 2>/dev/null || true
rm -rf /root/.docker 2>/dev/null || true
echo "📋 Paso 8: Limpiando logs..."
truncate -s 0 /var/log/*.log 2>/dev/null || true
rm -rf /var/log/docker 2>/dev/null || true
journalctl --vacuum=time=1d 2>/dev/null || true
echo "🗑️  Paso 9: Limpiando tmp..."
rm -rf /tmp/* 2>/dev/null || true
rm -rf /var/tmp/* 2>/dev/null || true
echo "📦 Paso 10: APT cleanup..."
apt-get autoremove -y 2>/dev/null || true
apt-get autoclean -y 2>/dev/null || true
echo ""
echo "=========================================="
echo "✅ LIMPIEZA COMPLETADA"
echo "=========================================="
echo ""
echo "Tu VPS está limpio. Espacio disponible:"
df -h /
echo ""
EOF
chmod +x /tmp/cleanup.sh
/tmp/cleanup.sh
'
```

**Espera a que termine.** Verás el mensaje `✅ LIMPIEZA COMPLETADA`.

---

### **PASO 4: Actualizar el Sistema**

```bash
apt update && apt upgrade -y
```

---

### **PASO 5: Instalar Docker y Dependencias**

```bash
# Instalar Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# Instalar Docker Compose
apt install -y docker-compose-plugin

# Instalar Git
apt install -y git

# Verificar
docker compose version
git --version
```

---

### **PASO 6: Crear Usuario y Clonar Proyecto**

```bash
# Crear usuario
useradd -m -d /home/impofer -s /bin/bash impofer
usermod -aG docker impofer

# Cambiar a ese usuario
su - impofer

# Clonar tu repositorio (CAMBIA LA URL)
git clone https://github.com/tu-usuario/tu-repo.git ~/app
cd ~/app

# Listar archivos
ls -la
```

---

### **PASO 7: Crear y Configurar .env**

Desde la carpeta `~/app`:

```bash
# Ver si existe .env.example
cat .env.example

# Copiar plantilla
cp .env.example .env

# Editar (abre el editor nano)
nano .env
```

**Edita los valores en nano:**
- Presiona `Ctrl+X` + `Y` + `Enter` para guardar

Valores necesarios:
```
DB_USERNAME=<db_user>
DB_PASSWORD=<db_password>
JWT_SECRET=<jwt_secret_min_64_chars>
JWT_EXPIRATION=14400000
STRIPE_API_KEY=<stripe_secret_key>
STRIPE_WEBHOOK_SECRET=<stripe_webhook_secret>
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<smtp_username>
MAIL_PASSWORD=<smtp_app_password>
ALLOWED_ORIGINS=https://impofer.com,https://www.impofer.com
APP_BASE_URL=https://impofer.com
NGINX_SERVER_NAME=impofer.com
```

---

### **PASO 8: Obtener Certificado SSL**

```bash
# Volver a root si es necesario
exit  # (si saliste del usuario impofer)

# Ir a la carpeta del proyecto
cd /home/impofer/app

# Iniciar solo Nginx
docker compose up -d nginx

# Esperar 5 segundos
sleep 5

# Obtener certificado (CAMBIA EL DOMINIO Y EMAIL)
docker run --rm \
  -v certbot_conf:/etc/letsencrypt \
  -v certbot_www:/var/www/certbot \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email admin@impofer.com \
    --agree-tos \
    --no-eff-email \
    -d impofer.com \
    -d www.impofer.com \
    --non-interactive
```

**Espera a que aparezca:** `Congratulations! Your certificate...`

---

### **PASO 9: Levantar Todo**

```bash
cd /home/impofer/app

# Detener y levantar todo
docker compose down
docker compose up -d --build

# Esperar que se inicie (10-15 segundos)
sleep 10

# Ver estado
docker compose ps

# Ver logs (Ctrl+C para salir)
docker compose logs -f app
```

Deberías ver algo como:
```
impofer-app   | 2026-03-02 18:32:45 INFO  Starting app...
impofer-app   | 2026-03-02 18:32:52 INFO  Application started on port 8080
```

---

### **PASO 10: Configurar Firewall**

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status
```

---

## ✅ Verificar que Todo Funciona

```bash
# Verificar health check
curl https://impofer.com/actuator/health

# Debería devolver:
# {"status":"UP"}

# Ver todos los servicios
docker compose ps

# Ver logs
docker compose logs -f
```

---

## 📍 URLs en Producción

| Recurso | URL |
|---------|-----|
| **Tienda** | `https://impofer.com` |
| **Login** | `https://impofer.com/login.html` |
| **Admin** | `https://impofer.com/admin.html` |
| **Health** | `https://impofer.com/actuator/health` |

---

## 🔧 Comandos Útiles para el Futuro

```bash
# Ver logs en tiempo real
docker compose logs -f

# Ver solo logs de la app
docker compose logs -f app

# Reiniciar solo la app
docker compose restart app

# Reiniciar todo
docker compose restart

# Ver estado de servicios
docker compose ps

# Detener todos los servicios
docker compose down

# Levantar nuevamente
docker compose up -d

# Actualizar a nuevo código
cd /home/impofer/app
git pull origin main
docker compose up -d --build

# Ver uso de disco
docker system df

# Limpiar imágenes viejas
docker image prune -a
```

---

## 🆘 Si Algo Falla

### **La app no inicia**
```bash
# Ver logs detallados
docker compose logs app

# Reiniciar app
docker compose restart app

# Revisar variables de entorno
cat .env
```

### **Certificado SSL no se genera**
```bash
# Ver logs de certbot
docker compose logs certbot

# Verificar que el puerto 80 está libre
netstat -tulpn | grep 80

# Obtener certificado manualmente
docker run --rm \
  -v certbot_conf:/etc/letsencrypt \
  -v certbot_www:/var/www/certbot \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email admin@impofer.com \
    --agree-tos \
    -d impofer.com \
    -d www.impofer.com
```

### **No puedo conectar con SSH**
- Verifica que el firewall permite el puerto 22: `ufw allow OpenSSH`
- Verifica la IP correcta del VPS
- Espera 2-3 minutos desde que ejecutaste el script de limpieza

---

## 📌 Resumen Visual del Flujo

```
┌─────────────────────────────────────────┐
│ 1. Conectar al VPS (SSH)                │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 2. Ejecutar CLEANUP SCRIPT              │
│    (Borra TODO)                         │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 3. Instalar Docker, Git                 │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 4. Clonar Repositorio                   │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 5. Crear .env con variables             │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 6. Obtener Certificado SSL              │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 7. Levantar con docker compose up -d    │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 8. Verificar: curl https://impofer.com  │
└─────────────────────────────────────────┘
      ✅ ¡LISTO!
```

---

## 💡 Notas Finales

- El servidor estará **DOWN durante todo el proceso** (no será accesible)
- Los certificados SSL se renuevan **automáticamente cada 12 horas**
- Backups de la BD: Crea uno antes de ejecutar la limpieza
- Si necesitas rollback, usa un snapshot de Hostinger (si lo tienen)

**¿Necesitas ayuda en algún paso? Pregunta en el chat.**
