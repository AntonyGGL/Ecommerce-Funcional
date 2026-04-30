# Guía de Despliegue — Hostinger VPS

> **Requisito**: Hostinger **VPS** (KVM) con Ubuntu 22.04 LTS.
> La app es Java/Spring Boot — el hosting compartido de Hostinger **no sirve**,
> ya que sólo soporta PHP. Necesitas mínimo el plan **KVM 1** (1 vCPU, 4 GB RAM).

---

## 1. Preparación en Hostinger

1. Entra al **hPanel** → Servidores VPS → crea/accede a tu VPS.
2. Anota la **IP pública** del VPS (ej. `168.100.x.x`).
3. Conecta tu **dominio** al VPS: en el panel DNS apunta los registros:
   ```
   A  @    → <IP_DEL_VPS>
   A  www  → <IP_DEL_VPS>
   ```
4. Espera 5–30 min a que la DNS se propague.

---

## 2. Acceso SSH e instalación de dependencias

```bash
# Conectar al VPS
ssh root@<IP_DEL_VPS>

# Actualizar sistema
apt update && apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# Instalar Docker Compose (plugin v2)
apt install -y docker-compose-plugin
docker compose version   # debe mostrar v2.x

# Instalar Git
apt install -y git

# (Opcional) Crear usuario sin root para mayor seguridad
adduser impofer
usermod -aG docker impofer
su - impofer
```

---

## 3. Subir el proyecto al VPS

### Opción A — Git (recomendado)
```bash
git clone https://github.com/<tu-usuario>/<tu-repo>.git /home/impofer/app
cd /home/impofer/app
```

### Opción B — SCP desde tu máquina local (Windows)
```powershell
# En tu PC local (PowerShell)
scp -r c:\Proyectos\demo root@<IP_DEL_VPS>:/home/impofer/app
```

---

## 4. Configurar variables de entorno

```bash
cd /home/impofer/app

# Copiar la plantilla
cp .env.example .env

# Editar con tus valores reales
nano .env
```

**Valores obligatorios a rellenar en `.env`:**

| Variable | Descripción |
|---|---|
| `NGINX_SERVER_NAME` | Tu dominio, ej. `impofer.com` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña segura para la BD |
| `JWT_SECRET` | Clave aleatoria ≥ 64 chars (`openssl rand -base64 64`) |
| `STRIPE_API_KEY` | `sk_live_...` de tu cuenta Stripe |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` de Stripe Dashboard |
| `MAIL_USERNAME` | Tu email de Gmail/SMTP |
| `MAIL_PASSWORD` | App Password de Gmail (no tu contraseña normal) |
| `ALLOWED_ORIGINS` | `https://impofer.com,https://www.impofer.com` |
| `APP_BASE_URL` | `https://impofer.com` |

---

## 5. Obtener certificado SSL (Let's Encrypt) — PRIMERA VEZ

Antes del primer `docker compose up`, necesitas obtener el certificado.
Descarga solo Certbot en Docker:

```bash
# Asegúrate de que el puerto 80 está libre
docker compose up -d nginx

# Obtener certificado con Certbot (cambia impofer.com y el email)
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
    -d www.impofer.com
```

---

## 6. Levantar todos los servicios

```bash
cd /home/impofer/app
docker compose up -d --build
```

Verificar que todos los contenedores están `Up`:
```bash
docker compose ps
```

Ver logs en tiempo real:
```bash
docker compose logs -f app
```

---

## 7. ⚠️ Actualizar la clave pública de Stripe en checkout.html

En [src/main/resources/static/checkout.html](src/main/resources/static/checkout.html) línea 225
está la clave de **test** de Stripe. Para producción debes reemplazarla
con tu clave **live**:

```html
<!-- ANTES (test) -->
const stripe = Stripe('pk_test_51Swl...');

<!-- DESPUÉS (producción) -->
const stripe = Stripe('pk_live_TU_CLAVE_PUBLICA_LIVE');
```

Tu clave pública live la encuentras en:
**Stripe Dashboard → Developers → API Keys → Publishable key**.

Después de cambiar el archivo, reconstruye la imagen:
```bash
docker compose up -d --build app
```

---

## 8. Verificar el despliegue

```bash
# Health check de la app
curl https://impofer.com/actuator/health

# Ver logs
docker compose logs -f

# Reiniciar un servicio
docker compose restart app
```

---

## 9. Firewall (ufw)

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status
```

---

## 10. Renovación automática de SSL

El servicio `certbot` en docker-compose ya se encarga de renovar
el certificado cada 12 horas automáticamente. No se requiere acción manual.

Para recargar Nginx después de una renovación manual:
```bash
docker compose exec nginx nginx -s reload
```

---

## Resumen de URLs en producción

| Recurso | URL |
|---|---|
| Tienda | `https://impofer.com` |
| Login | `https://impofer.com/login.html` |
| Panel Admin | `https://impofer.com/admin.html` |
| Health Check | `https://impofer.com/actuator/health` |
