# Quarkus Docker Demo — Workshop de Contenedores

API REST con Quarkus 3 + Java 21 diseñada para demostrar
la diferencia entre un Dockerfile básico y uno multi-stage.

## Estructura del proyecto

```
quarkus-demo/
├── src/
│   ├── main/
│   │   ├── java/org/workshop/demo/
│   │   │   ├── model/Product.java
│   │   │   ├── resource/ProductResource.java   # GET/POST/DELETE /api/products
│   │   │   └── resource/GreetingResource.java  # GET /api
│   │   └── resources/application.properties
│   └── test/...
├── pom.xml                     # Quarkus 3.10 + RESTEasy + Health + Micrometer
├── Dockerfile.basic            # imagen única, todo dentro
├── Dockerfile.multistage       # builder Maven + runtime JRE Alpine
└── Dockerfile.multistage.ubi   # variante Red Hat UBI (para OpenShift)
```

## Endpoints disponibles

| Método | URL               | Descripción          |
|--------|-------------------|----------------------|
| GET    | /api              | Info de la app       |
| GET    | /api/products     | Listar productos     |
| POST   | /api/products     | Crear producto       |
| DELETE | /api/products/:id | Eliminar producto    |
| GET    | /health           | Health check         |
| GET    | /health/live      | Liveness probe       |
| GET    | /health/ready     | Readiness probe      |

## Pasos del demo

### 1. Construir ambas imágenes

```bash
# Básico (puede tardar 3-5 min — descarga JDK + instala Maven via apt)
docker build -f Dockerfile.basic -t quarkus-demo:basic .

# Multi-stage (primera vez descarga maven:3.9-eclipse-temurin-21)
docker build -f Dockerfile.multistage -t quarkus-demo:prod .
```

### 2. El momento "wow" — comparar tamaños

```bash
docker images | grep quarkus-demo
```

Resultado esperado:
```
quarkus-demo   prod    xxxxxxxxxx   2 min ago    ~242 MB
quarkus-demo   basic   xxxxxxxxxx   8 min ago    ~856 MB
```

Reducción: ~72% menos de tamaño.

### 3. ¿Qué hay dentro de cada imagen?

```bash
# ¿Maven está en producción? Básico: sí. Multi-stage: no
docker run --rm quarkus-demo:basic  which mvn
docker run --rm quarkus-demo:prod   which mvn

# ¿Código fuente .java está expuesto?
docker run --rm quarkus-demo:basic  find /app/src -name "*.java" 2>/dev/null
docker run --rm quarkus-demo:prod   find /app/src -name "*.java" 2>/dev/null

# ¿El JDK (compilador) está en la imagen de producción?
docker run --rm quarkus-demo:basic  javac --version
docker run --rm quarkus-demo:prod   javac --version   # debería fallar

# Ver qué hay en /app de producción
docker run --rm quarkus-demo:prod   ls -lh /app
# Solo: quarkus-run.jar, lib/, app/, quarkus/
```

### 4. Inspeccionar capas

```bash
# Ver cuánto pesa cada capa (RUN, COPY)
docker history quarkus-demo:basic
docker history quarkus-demo:prod

# Análisis detallado con dive (si está instalado)
dive quarkus-demo:basic
dive quarkus-demo:prod
```

### 5. Ambas apps funcionan igual

```bash
docker run -d -p 8081:8080 --name app-basic quarkus-demo:basic
docker run -d -p 8082:8080 --name app-prod  quarkus-demo:prod

# Esperar arranque (~3s) y probar
curl http://localhost:8081/api
curl http://localhost:8082/api

curl http://localhost:8081/api/products
curl http://localhost:8082/api/products

curl http://localhost:8081/health
curl http://localhost:8082/health

docker stop app-basic app-prod
docker rm   app-basic app-prod
```

### 6. Para OpenShift (variante UBI)

```bash
docker build -f Dockerfile.multistage.ubi -t quarkus-demo:ubi .
docker images | grep quarkus-demo
```

## Resumen de diferencias

| | Básico | Multi-stage | Multi-stage UBI |
|---|---|---|---|
| Tamaño | ~856 MB | ~242 MB | ~307 MB |
| OS base | Debian | Alpine | UBI 9 |
| JDK en prod | Sí | No (solo JRE) | No (solo JRE) |
| Maven en prod | Sí (~200 MB) | No | No |
| .m2 en prod | Sí (~180 MB) | No | No |
| Código fuente | Expuesto | No | No |
| Usuario root | Sí | No (appuser) | No (1001) |
| Certificado RHEL | No | No | Sí |

## Truco de cache de Maven en multi-stage

El Dockerfile.multistage copia el `pom.xml` antes que el código fuente:

```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline -q   # ← esta capa se cachea

COPY src ./src                     # cambiar código no invalida la capa anterior
RUN mvn package -DskipTests -q
```

Si solo cambia el código Java (no el pom.xml), Docker reutiliza la capa
de dependencias y el rebuild tarda segundos en vez de minutos.
