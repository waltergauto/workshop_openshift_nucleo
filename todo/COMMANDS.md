# Comandos para ejecutar con Podman

## Construir y levantar todos los servicios
podman-compose -f podman-compose.yml up -d --build

## Ver logs de todos los servicios
podman-compose -f podman-compose.yml logs -f

## Ver logs de un servicio específico
podman-compose -f podman-compose.yml logs -f proxy
podman-compose -f podman-compose.yml logs -f backend
podman-compose -f podman-compose.yml logs -f frontend
podman-compose -f podman-compose.yml logs -f postgres
podman-compose -f podman-compose.yml logs -f jaeger

## Ver estado de los servicios
podman-compose -f podman-compose.yml ps

## Detener todos los servicios
podman-compose -f podman-compose.yml down

## Reconstruir y reiniciar un servicio
podman-compose -f podman-compose.yml up -d --build backend
podman-compose -f podman-compose.yml up -d --build frontend
podman-compose -f podman-compose.yml up -d --build proxy

## Reconstruir todo
podman-compose -f podman-compose.yml up -d --build

## Eliminar volúmenes (CUIDADO: elimina la base de datos)
podman-compose -f podman-compose.yml down -v

## URLs de los servicios
# Proxy/Frontend: http://localhost:8080/
# Backend API: http://localhost:8080/todos
# Health Check: http://localhost:8080/health
# Jaeger UI: http://localhost:16686/
# Jaeger OTLP gRPC: localhost:4317
# Jaeger OTLP HTTP: localhost:4318

## Generar trazas de prueba
for i in {1..10}; do curl -s http://localhost:8080/todos > /dev/null; done
