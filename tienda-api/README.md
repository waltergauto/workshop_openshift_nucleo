# TiendaAPI — Workshop OpenShift 4.18

Aplicación de referencia para el módulo de **resiliencia y despliegue** del workshop de OpenShift orientado a desarrollo.

## Estructura del repositorio

```
tienda-api/
├── v1/                     # Código fuente versión 1 (blue)
│   ├── app.py
│   └── requirements.txt
├── v2/                     # Código fuente versión 2 (green/canary)
│   ├── app.py              # + paginación y filtro por categoría
│   └── requirements.txt
└── manifests/
    ├── 00-runbook.sh       # Guía paso a paso de todos los ejercicios
    ├── 01-build.yaml       # ImageStream + BuildConfig S2I (v1 y v2)
    ├── 02-deployments.yaml # Deployment blue (v1) + green (v2)
    ├── 03-services-route.yaml # Services + Route con traffic split
    ├── 04-hpa.yaml         # HorizontalPodAutoscaler
    └── 05-pdb.yaml         # PodDisruptionBudget
```

## Endpoints de la API

| Endpoint | v1 | v2 |
|---|---|---|
| `GET /` | Info general | Info general |
| `GET /productos` | Lista todos | Lista con **paginación** (`?page=1&limit=5`) |
| `GET /productos?categoria=X` | ❌ | ✅ Filtro por categoría |
| `GET /productos/{id}` | Detalle | Detalle |
| `GET /version` | `{"version":"v1","color":"blue"}` | `{"version":"v2","color":"green"}` |
| `GET /health` | `{"status":"ok"}` | `{"status":"ok"}` |
| `GET /stress?seconds=N` | Genera carga CPU | Genera carga CPU |

## Flujo del workshop

```
[Build S2I v1 + v2]
       ↓
[Deploy blue (v1)] → [Aplicar PDB] → [Ejercicio de bloqueo]
       ↓
[Aplicar HPA] → [Generar carga] → [Observar escalado]
       ↓
[Canary 10%] → [Canary 50%] → [Canary 100%]
       ↓
[Blue-Green switch] → [Rollback] → [Cleanup blue]
```

## Requisitos del clúster

- OpenShift 4.18 / Kubernetes 1.31
- ImageStream `python:3.11-ubi9` disponible en namespace `openshift`
- Métricas habilitadas (metrics-server) para HPA
- Acceso `cluster-admin` o role con permisos de `adm drain` para el ejercicio PDB

## Variables a ajustar antes de aplicar

En `manifests/01-build.yaml`:
```yaml
spec.source.git.uri: https://github.com/TU_ORG/tienda-api.git
```

## Comandos rápidos de verificación

```bash
# Estado general
oc get deployment,hpa,pdb,route -l app=tienda-api

# Distribución de tráfico
ROUTE=$(oc get route tienda-api -o jsonpath='{.spec.host}')
for i in $(seq 1 20); do
  curl -sk https://$ROUTE/version | python3 -c \
    "import sys,json; d=json.load(sys.stdin); print(d['version'])"
done | sort | uniq -c

# Ver split actual de la route
oc get route tienda-api \
  -o jsonpath='blue={.spec.to.weight} green={.spec.alternateBackends[0].weight}'
```
