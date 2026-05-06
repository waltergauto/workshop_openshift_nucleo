# Proyecto Astro + Quarkus

Este proyecto contiene un frontend en Astro y un backend en Quarkus (Java 21).

## Estructura

```
.
├── frontend/          # Aplicación Astro
│   └── src/
│       └── pages/
│           └── index.astro
└── backend/           # API REST con Quarkus
    └── src/
        └── main/
            └── java/com/example/backend/
```

## Requisitos

- Node.js 22+ (para el frontend)
- Java 21 (para el backend)
- Maven 3.8+ (para el backend)

## Ejecución

### Backend (Quarkus)

```bash
cd backend
mvn quarkus:dev
```

El backend estará disponible en: http://localhost:8080

Endpoints:
- `GET /api/v1/hello` - Mensaje de saludo
- `GET /api/v1/empleados` - Lista de empleados

### Health Checks (OpenShift)

**Backend (Quarkus):**
- Liveness: `GET /q/health/live`
- Readiness: `GET /q/health/ready`
- Health UI: `GET /q/health` (interfaz visual)

**Frontend (Astro):**
- Liveness: `GET /api/healthz`
- Readiness: `GET /api/readyz` (verifica también conexión con backend)

### Frontend (Astro)

```bash
cd frontend
npm install
npm run dev
```

El frontend estará disponible en: http://localhost:4321

## Comunicación

El frontend se comunica con el backend mediante peticiones fetch a la API REST en formato JSON.
