# tienda-otel — Workshop OpenTelemetry en OpenShift

Escenario pedagógico de tres microservicios Quarkus con trazas distribuidas
end-to-end visibles en Jaeger/Tempo a través del operador
**Red Hat build of OpenTelemetry**.

---

## Arquitectura

```
[Route OCP]
     │
     ▼
 pedido-api  ──────────────────────────────────────┐
 (Quarkus)                                         │
     │                                             │
     ├── GET /productos/{id}  ──►  producto-api    │
     │                             (Quarkus)       │
     ├── PUT /productos/{id}/stock ──► (mismo)     │ PostgreSQL 16
     │                                      ▼      │  schemas:
     └── POST /notificaciones ─► notificacion-api  │  · productos
                                  (Quarkus)        │  · pedidos
                                         ▼         │  · notificaciones
                                    [PostgreSQL] ◄─┘
```

Un `POST /pedidos` genera una traza con **4 spans** en árbol:

```
POST /pedidos                              [pedido-api]
  └─ GET /productos/{id}                  [producto-api]
  └─ PUT /productos/{id}/stock            [producto-api]
  └─ POST /notificaciones                 [notificacion-api]
```

---

## Stack técnico

| Componente    | Tecnología                        |
|---------------|-----------------------------------|
| Runtime       | Quarkus 3.15.1 / Java 21          |
| REST server   | quarkus-rest + quarkus-rest-jackson |
| REST client   | quarkus-rest-client-jackson       |
| ORM           | quarkus-hibernate-orm-panache     |
| BD driver     | quarkus-jdbc-postgresql           |
| Tracing       | quarkus-opentelemetry (OTLP/gRPC) |
| Health        | quarkus-smallrye-health           |
| Métricas      | quarkus-micrometer-registry-prometheus |
| Logging       | quarkus-logging-json              |
| Base de datos | PostgreSQL 16                     |

---

## Despliegue en OpenShift

### 1. Crear namespace y PostgreSQL

```bash
oc apply -f manifests/00-namespace.yaml
oc apply -f manifests/01-postgresql.yaml

# Esperar que PostgreSQL esté listo
oc rollout status deployment/postgresql -n tienda-otel
```

### 2. Construir y desplegar producto-api

```bash
# Compilar localmente
cd producto-api
mvn package -DskipTests

# Iniciar build en OCP (binary build)
oc apply -f ../manifests/02-producto-api.yaml
oc start-build producto-api --from-dir=. -n tienda-otel --follow

# Desplegar
oc rollout status deployment/producto-api -n tienda-otel
cd ..
```

### 3. Construir y desplegar notificacion-api

```bash
cd notificacion-api
mvn package -DskipTests
oc apply -f ../manifests/04-notificacion-api.yaml
oc start-build notificacion-api --from-dir=. -n tienda-otel --follow
oc rollout status deployment/notificacion-api -n tienda-otel
cd ..
```

### 4. Construir y desplegar pedido-api

```bash
cd pedido-api
mvn package -DskipTests
oc apply -f ../manifests/03-pedido-api.yaml
oc start-build pedido-api --from-dir=. -n tienda-otel --follow
oc rollout status deployment/pedido-api -n tienda-otel
cd ..
```

> **Nota sobre imagen base:** Si el clúster no tiene acceso a
> `registry.redhat.io`, reemplazar en el Dockerfile:
> ```
> FROM eclipse-temurin:21-jre-ubi9-minimal
> ```

---

## Verificación funcional

```bash
# Obtener URL pública de pedido-api
PEDIDO_URL=$(oc get route pedido-api -n tienda-otel -o jsonpath='{.spec.host}')

# 1. Crear un producto
curl -s -X POST https://$PEDIDO_URL/../producto-api/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Laptop","descripcion":"Laptop 16GB","precio":1500.00,"stock":10}' | jq .

# O directamente contra el servicio interno (desde dentro del clúster):
oc run curl --image=curlimages/curl -it --rm --restart=Never -n tienda-otel -- \
  curl -s -X POST http://producto-api:8080/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Laptop","descripcion":"Laptop 16GB","precio":1500.00,"stock":10}'

# 2. Crear un pedido (genera la traza completa de 4 spans)
oc run curl --image=curlimages/curl -it --rm --restart=Never -n tienda-otel -- \
  curl -s -X POST http://pedido-api:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"cantidad":2,"clienteNombre":"Juan Perez"}'

# 3. Listar pedidos
oc run curl --image=curlimages/curl -it --rm --restart=Never -n tienda-otel -- \
  curl -s http://pedido-api:8080/pedidos

# 4. Listar notificaciones
oc run curl --image=curlimages/curl -it --rm --restart=Never -n tienda-otel -- \
  curl -s http://notificacion-api:8080/notificaciones

# 5. Probar stock insuficiente (debe devolver 409)
oc run curl --image=curlimages/curl -it --rm --restart=Never -n tienda-otel -- \
  curl -s -X POST http://pedido-api:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"cantidad":999,"clienteNombre":"Stock Test"}'
```

---

## Endpoints por servicio

### producto-api
| Método | Path                       | Descripción              |
|--------|----------------------------|--------------------------|
| GET    | /productos                 | Listar productos         |
| GET    | /productos/{id}            | Detalle del producto     |
| POST   | /productos                 | Crear producto           |
| PUT    | /productos/{id}/stock      | Actualizar stock (delta) |
| GET    | /health/ready              | Readiness probe          |
| GET    | /health/live               | Liveness probe           |
| GET    | /q/metrics                 | Métricas Prometheus      |

### pedido-api
| Método | Path                       | Descripción              |
|--------|----------------------------|--------------------------|
| GET    | /pedidos                   | Listar pedidos           |
| GET    | /pedidos/{id}              | Detalle del pedido       |
| POST   | /pedidos                   | Crear pedido             |
| PUT    | /pedidos/{id}/estado       | Actualizar estado        |
| GET    | /health/ready              | Readiness probe          |
| GET    | /health/live               | Liveness probe           |
| GET    | /q/metrics                 | Métricas Prometheus      |

### notificacion-api
| Método | Path                       | Descripción              |
|--------|----------------------------|--------------------------|
| GET    | /notificaciones            | Listar notificaciones    |
| POST   | /notificaciones            | Registrar notificación   |
| GET    | /health/ready              | Readiness probe          |
| GET    | /health/live               | Liveness probe           |
| GET    | /q/metrics                 | Métricas Prometheus      |

---

## Próximo paso: OpenTelemetry Operator

Ver **manifests/05-otel-collector.yaml** (a generar) para:
- Instalar el operador **Red Hat build of OpenTelemetry**
- Crear un `OpenTelemetryCollector` en modo `Deployment`
- Exportar trazas a Jaeger (incluido en el operador Tempo/Jaeger)

Las tres apps ya están configuradas para enviar trazas OTLP/gRPC a
`http://otel-collector:4317`. Solo hace falta que el collector exista.

---

## Variables de entorno de referencia

| Variable                      | Default                        | Descripción                    |
|-------------------------------|--------------------------------|--------------------------------|
| `DB_HOST`                     | `localhost`                    | Host de PostgreSQL             |
| `DB_PORT`                     | `5432`                         | Puerto de PostgreSQL           |
| `DB_NAME`                     | `tienda`                       | Nombre de la base de datos     |
| `DB_USER`                     | `workshop`                     | Usuario de BD                  |
| `DB_PASSWORD`                 | `workshop123`                  | Contraseña de BD               |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://otel-collector:4317`   | Endpoint del collector OTLP    |
| `PRODUCTO_API_URL`            | `http://producto-api:8080`     | URL interna de producto-api    |
| `NOTIFICACION_API_URL`        | `http://notificacion-api:8080` | URL interna de notificacion-api|
