# Workshop OpenShift — Microservicios

Stack: React + Spring Boot + PostgreSQL

## Estructura


## API

| Metodo | Ruta              | Descripcion           |
|--------|-------------------|-----------------------|
| GET    | `/api/products`   | Lista todos los productos |
| GET    | `/api/products/{id}` | Obtiene un producto por ID |
| GET    | `/actuator/health` | Health check          |

## Variables de entorno — Backend

| Variable      | Default     | Descripción          |
|---------------|-------------|----------------------|
| `DB_HOST`     | localhost   | PostgreSQL Host      |
| `DB_PORT`     | 5432        | Port                 |
| `DB_NAME`     | productsdb  | Database Name        |
| `DB_USER`     | postgres    | User                 |
| `DB_PASSWORD` | postgres    | Password             |
| `PORT`        | 8080        | Server Port          |

## Build local con Podman

```bash
# Backend
cd backend
podman build -t products-api:latest .

# Frontend (apuntando al backend local)
cd frontend
podman build --build-arg VITE_API_URL=http://localhost:8080 -t products-frontend:latest .
```

## Despliegue en OpenShift

```bash
# Crear proyecto
oc new-project workshop

# Aplicar manifiestos en orden
oc apply -f k8s/01-postgres.yaml
oc apply -f k8s/02-backend.yaml
oc apply -f k8s/03-frontend.yaml

# Ver las rutas generadas
oc get routes
```

> **Nota**: Antes de aplicar `02-backend.yaml` y `03-frontend.yaml`,
> reemplazá el campo `image:` con la ruta real de tu registry.
