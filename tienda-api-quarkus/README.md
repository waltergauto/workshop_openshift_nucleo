# TiendaAPI — Quarkus / Java 21
## Workshop OpenShift 4.18

Versión Quarkus del escenario end-to-end. Mismos módulos que la versión Python/Flask
pero con stack Java 21 + Quarkus 3.15 (fast-jar).

---

## Estructura

```
tienda-api-quarkus/
├── v1/                         ← REST básico
│   ├── pom.xml                 (Quarkus 3.15.1, Java 21)
│   └── src/main/
│       ├── java/.../resource/ProductoResource.java
│       └── resources/application.properties
│
├── v2/                         ← + paginación + filtro por categoría
│   └── (misma estructura)
│
├── v3/                         ← + JSON logging + correlation ID + métricas
│   └── src/main/java/.../
│       ├── resource/ProductoResource.java
│       └── filter/
│           ├── CorrelationIdFilter.java   ← genera/propaga X-Correlation-ID
│           └── RequestLoggingFilter.java  ← log de access por request
│
├── dockerfiles/
│   ├── Dockerfile.bad          ← corre como root → falla en OCP (ejercicio SCC)
│   └── Dockerfile.good         ← multi-stage UBI9, rootless, OCP-ready
│
└── manifests/
    ├── 00-runbook.sh           ← guía completa de todos los ejercicios
    ├── 01-build.yaml           ← ImageStream + BuildConfig (v1, v2, v3, v3-bad)
    └── 02-workloads.yaml       ← Deployment + Service + Route + HPA + PDB
```

---

## Endpoints por versión

| Endpoint | v1 | v2 | v3 |
|---|---|---|---|
| `GET /` | ✅ | ✅ | ✅ |
| `GET /productos` | Lista simple | Paginación + filtro | Igual + logs JSON |
| `GET /productos/{id}` | ✅ | ✅ | ✅ + log WARNING en 404 |
| `GET /version` | `v1/blue` | `v2/green` | `v3/green` |
| `GET /health/ready` | ✅ SmallRye | ✅ | ✅ |
| `GET /health/live` | ✅ | ✅ | ✅ |
| `GET /stress?seconds=N` | CPU load | CPU load | CPU load |
| `GET /error-demo` | ❌ | ❌ | ✅ → stderr |
| `GET /q/metrics` | ❌ | ❌ | ✅ Prometheus |
| `X-Correlation-ID` | ❌ | ❌ | ✅ MDC + header |

---

## Diferencias clave vs versión Python/Flask

| Aspecto | Python/Flask | Quarkus/Java 21 |
|---|---|---|
| Arranque | ~0.5s | ~2-3s (JVM warm-up) |
| Memory request | 128Mi | 256Mi |
| Health path | `/health` | `/health/ready` + `/health/live` |
| JSON logging | `logger.py` custom | `quarkus-logging-json` (extensión) |
| Correlation ID | middleware Flask | `ContainerRequestFilter` JAX-RS |
| Métricas | ❌ | `/q/metrics` (Micrometer/Prometheus) |
| UID imagen | 1001 | 185 (UBI OpenJDK) |

---

## Desarrollo local

```bash
# Modo dev (hot reload, logs legibles)
cd v3
./mvnw quarkus:dev

# Build fast-jar
./mvnw package -DskipTests

# Correr el jar
java -jar target/quarkus-app/quarkus-run.jar
```

---

## Ajuste requerido antes de usar

En `manifests/01-build.yaml`, cambiar:
```yaml
uri: https://github.com/TU_ORG/tienda-api-quarkus.git
```
