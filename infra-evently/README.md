# App evently — Infra

## Inicio local en Windows

Con Docker Desktop iniciado, ejecuta `./start-local.ps1` desde PowerShell. El script crea `.env` con claves locales si hace falta, prepara la red y espera a que los servicios estén listos. Android accede a la API REST directamente en el puerto 3001. El contenedor web sirve sus archivos estáticos en el puerto 3000. [Swagger local](http://localhost:3001/api/docs) · [Sitio web local](http://localhost:3000)

Si otro servicio usa el puerto de Redis del equipo, ajusta `REDIS_PORT` en `.env`. Redis mantiene el puerto 6379 dentro de la red Docker.

## Este directorio contiene los contenedores configurados necesarios para poder aproveer y deployar las aplicaciones de evently

## Paso 0) Instalación de Docker y Docker Compose

Antes de continuar, asegúrate de tener instalados **Docker Engine** y **Docker Compose v2** en tu máquina:

### Windows

1. Descarga e instala **Docker Desktop** desde: [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
2. Verifica la instalación en PowerShell o CMD:
    ```bash
    docker --version
    docker compose version
    ```

### Linux (Ubuntu/Debian como ejemplo)

1. Instala Docker:
    ```bash
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl gnupg lsb-release
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg]    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"    | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    ```
2. Verifica la instalación:
    ```bash
    docker --version
    docker compose version
    ```

### macOS

1. Descarga e instala **Docker Desktop para Mac** desde: [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)  
   (Soporta tanto Intel como Apple Silicon).
2. Verifica la instalación en la terminal:
    ```bash
    docker --version
    docker compose version
    ```

---

## Estructura del directorio

```
infra
├── docker-compose.mysql.yml   # Servicio Mysql (development)
├── docker-compose.redis.yml   # Servicio Redis (development)
```

---

## Requisitos

-   Docker Engine **20.10+** (o superior)
-   Docker Compose **v2** (plugin oficial de Docker)
-   Red externa compartida (p. ej. `network-app-evently`) para vincular servicios entre archivos Compose

---

## Variables de entorno

El script de Windows genera `.env` desde la plantilla y asigna claves locales para MySQL y JWT. Para un arranque manual, copia la plantilla a `.env` y completa esas claves antes de iniciar los servicios. [Plantilla de configuración](.env.example)

---

## Red compartida (una sola vez)

Crea la red externa (si no existe). Todas las composiciones la referencian como `external: true`:

```bash
docker network create network-app-evently
```

> Puedes verificar con `docker network ls`. Si la red ya existe, este comando fallará inofensivamente.

---

## Orden recomendado de arranque

> **Importante**: El servicio **HTTP** ejecuta **migraciones** automáticamente antes de iniciar (entrypoint). Asegúrate de que Mysql esté **arriba** antes de levantar HTTP.

### 1) Mysql

```bash
docker-compose -f docker-compose.mysql.yml -p app-evently up -d
```

### 2) Redis

```bash
docker-compose -f docker-compose.redis.yml -p app-evently up -d
```

### 3) HTTP (API)

```bash
docker-compose -f docker-compose.http.yml -p app-evently up -d
docker-compose -f docker-compose.http.yml -p app-evently logs -f http
```

### 4) WEB (Vuejs)

```bash
docker-compose -f docker-compose.web.yml -p app-evently up -d
docker-compose -f docker-compose.web.yml -p app-evently logs -f web
```

---

## Puertos por servicio (host → container)

Los puertos se publican en `127.0.0.1`. La tabla usa la configuración predeterminada.

| Servicio | Puerto del equipo | Puerto del contenedor |
| --- | ---: | ---: |
| Web estática (Nginx) | 3000 | 80 |
| API REST | 3001 | 3001 |
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |

Si configuras `REDIS_PORT=6382`, Redis queda en `6382 → 6379`.
