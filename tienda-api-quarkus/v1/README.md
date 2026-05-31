TiendaAPI — Resumen para el workshop
Stack

Quarkus 3.15.1 sobre Java 21, empaquetado como fast-jar
Logging JSON estructurado vía quarkus-logging-json + MDC
Correlation ID propagado automáticamente en cada request


Endpoints
Tráfico normal → logs INFO en stdout
bash# Listado completo
GET /productos

# Paginación
GET /productos?page=1&limit=5

# Filtro por categoría  →  log "productos_filtrados" con campo extra
GET /productos?categoria=perifericos
GET /productos?categoria=audio
GET /productos?categoria=computacion

# Detalle de producto existente
GET /productos/1
GET /productos/3

# Info de versión y color (útil para canary/blue-green)
GET /version
Errores de negocio → logs WARNING en stdout
bash# Producto inexistente → 404 + log "producto_not_found" con productoId
GET /productos/9999
GET /productos/0
Carga de CPU → útil para demostrar HPA
bash# Genera carga durante N segundos (máx 30)
GET /stress?seconds=5
GET /stress?seconds=15
Error no controlado → log ERROR en stderr
bash# Lanza RuntimeException → aparece en rojo en la consola de OCP
GET /error-demo
Health checks → sin log (sondas de Kubernetes)
bashGET /health/ready   # readinessProbe
GET /health/live    # livenessProbe

Secuencia sugerida para mostrar logs en vivo
bashROUTE=$(oc get route tienda-api -o jsonpath='{.spec.host}')

# 1. Abrir stream de logs en una terminal
oc logs -f -l app=tienda-api,version=green | jq .

# 2. En otra terminal, lanzar los requests en orden:

# INFO normal
curl -sk https://$ROUTE/productos

# INFO con campo extra (categoria + total)
curl -sk https://$ROUTE/productos?categoria=audio

# WARNING (404)
curl -sk https://$ROUTE/productos/9999

# ERROR (va a stderr, distinto stream)
curl -sk https://$ROUTE/error-demo

# Correlation ID propio — rastreable en los logs
curl -sk -H "X-Correlation-ID: demo-clase-01" https://$ROUTE/productos
