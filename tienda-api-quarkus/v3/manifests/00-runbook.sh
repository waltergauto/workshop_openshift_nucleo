#!/usr/bin/env bash
# ============================================================
#  RUNBOOK COMPLETO — tienda-api Quarkus / Java 21
#  OpenShift 4.18 | HPA · Blue-Green · Canary · PDB · Logging · SCC
# ============================================================

NAMESPACE="tienda-api-workshop"
ROUTE=""   # se rellena automáticamente después del deploy

# ============================================================
#  PASO 0 — Namespace y permisos
# ============================================================
oc new-project $NAMESPACE

# Ver el rango de UIDs que OCP asignará a este namespace
oc get namespace $NAMESPACE \
  -o jsonpath='{.metadata.annotations.openshift\.io/sa\.scc\.uid-range}'


# ============================================================
#  PASO 1 — Build
# ============================================================
oc apply -f manifests/01-build.yaml

# Lanzar los builds (S2I/Docker strategy)
oc start-build tienda-api-v1 --follow -n $NAMESPACE
oc start-build tienda-api-v2 --follow -n $NAMESPACE
oc start-build tienda-api-v3 --follow -n $NAMESPACE

# Verificar imágenes disponibles
oc get istag -n $NAMESPACE


# ============================================================
#  PASO 2 — Deploy baseline
# ============================================================
oc apply -f manifests/02-workloads.yaml -n $NAMESPACE
oc rollout status deployment/tienda-api-blue -n $NAMESPACE

ROUTE=$(oc get route tienda-api -n $NAMESPACE -o jsonpath='{.spec.host}')
echo "URL: https://$ROUTE"

# Quarkus: el /health/ready espera a que la app esté lista
curl -sk https://$ROUTE/health/ready | jq .
curl -sk https://$ROUTE/version      | jq .


# ============================================================
#  PASO 3 — PDB (ejercicio de bloqueo)
# ============================================================
oc get pdb tienda-api-pdb -n $NAMESPACE

# Demostrar bloqueo: cambiar a minAvailable: 2 con 2 réplicas
# Editar manifests/02-workloads.yaml temporalmente y re-aplicar
NODE=$(oc get pods -l app=tienda-api,version=blue -n $NAMESPACE \
       -o jsonpath='{.items[0].spec.nodeName}')
oc adm drain $NODE --pod-selector=app=tienda-api \
   --ignore-daemonsets --dry-run

# Resolver
oc scale deployment tienda-api-blue --replicas=3 -n $NAMESPACE


# ============================================================
#  PASO 4 — HPA (carga con /stress)
# ============================================================
oc get hpa tienda-api-hpa -n $NAMESPACE

# Generar carga
oc run load-gen \
  --image=registry.access.redhat.com/ubi9/ubi-minimal \
  --restart=Never --rm -it -n $NAMESPACE -- \
  sh -c "while true; do curl -sk https://$ROUTE/stress?seconds=2 >/dev/null; done"

# Observar escalado (otra terminal)
watch -n10 "oc get hpa tienda-api-hpa -n $NAMESPACE && \
            echo && \
            oc get pods -l app=tienda-api,version=blue -n $NAMESPACE"

# Detener carga
oc delete pod load-gen -n $NAMESPACE --ignore-not-found


# ============================================================
#  PASO 5 — Canary con v3 (logging + correlation)
# ============================================================
# Actualizar green para que apunte a v3
oc set image deployment/tienda-api-green \
  tienda-api=tienda-api:v3 -n $NAMESPACE
oc rollout status deployment/tienda-api-green -n $NAMESPACE

# Fase 1: 10% canary
oc set route-backends tienda-api \
  tienda-api-blue=90 tienda-api-green=10 -n $NAMESPACE

# Verificar distribución
echo "--- Distribución (30 llamadas) ---"
for i in $(seq 1 30); do
  curl -sk https://$ROUTE/version | python3 -c \
    "import sys,json; d=json.load(sys.stdin); print(d['version'])"
done | sort | uniq -c

# Fase 2 → 50/50
oc set route-backends tienda-api \
  tienda-api-blue=50 tienda-api-green=50 -n $NAMESPACE

# Fase 3 → 100% green
oc set route-backends tienda-api \
  tienda-api-blue=0 tienda-api-green=100 -n $NAMESPACE


# ============================================================
#  PASO 6 — Blue-Green switch + rollback
# ============================================================
curl -sk https://$ROUTE/version | jq .version   # → v3

# Rollback de emergencia (< 1 segundo)
oc set route-backends tienda-api \
  tienda-api-blue=100 tienda-api-green=0 -n $NAMESPACE

curl -sk https://$ROUTE/version | jq .version   # → v1

# Switch definitivo a v3
oc set route-backends tienda-api \
  tienda-api-blue=0 tienda-api-green=100 -n $NAMESPACE

oc scale deployment tienda-api-green --replicas=3 -n $NAMESPACE
oc scale deployment tienda-api-blue  --replicas=0 -n $NAMESPACE


# ============================================================
#  MÓDULO LOGGING — ejercicios con v3
# ============================================================

# A — Ver logs en JSON
oc logs -l app=tienda-api,version=green -n $NAMESPACE --tail=10

# B — Separar stdout (INFO) y stderr (ERROR)
# Solo INFO/WARNING → stdout
oc logs -l app=tienda-api,version=green -n $NAMESPACE 2>/dev/null | tail -5

# Solo ERROR → stderr
oc logs -l app=tienda-api,version=green -n $NAMESPACE 1>/dev/null

# C — Generar un ERROR (va a stderr)
curl -sk https://$ROUTE/error-demo
oc logs -l app=tienda-api,version=green -n $NAMESPACE 1>/dev/null | tail -3

# D — Correlation ID: rastrear un request específico
CORR_ID="workshop-quarkus-$(date +%s)"
curl -sk -H "X-Correlation-ID: $CORR_ID" https://$ROUTE/productos >/dev/null

oc logs -l app=tienda-api,version=green -n $NAMESPACE | \
  grep "$CORR_ID" | jq '{msg: .message, status: .mdc.status, ms: .mdc.latencyMs}'

# E — Análisis con jq
# Todos los requests con status >= 400
oc logs -l app=tienda-api,version=green -n $NAMESPACE | \
  jq 'select(.mdc.status != null and (.mdc.status | tonumber) >= 400)' 2>/dev/null

# Promedio de latencia
oc logs -l app=tienda-api,version=green -n $NAMESPACE | \
  jq -s '[.[] | select(.mdc.latencyMs != null) | .mdc.latencyMs | tonumber] | add/length' 2>/dev/null


# ============================================================
#  MÓDULO SCC — contenedores rootless
# ============================================================

# A — Demostrar que la imagen "bad" falla en OCP
oc create deployment demo-bad \
  --image=tienda-api:v3-bad --replicas=1 -n $NAMESPACE

oc get pods -l app=demo-bad -n $NAMESPACE
oc describe pod -l app=demo-bad -n $NAMESPACE | grep -A5 "Events"
# → Error: container has runAsNonRoot and image will run as root

oc delete deployment demo-bad -n $NAMESPACE

# B — Ver qué SCC usa la imagen correcta
POD=$(oc get pods -l app=tienda-api,version=green -n $NAMESPACE \
      -o jsonpath='{.items[0].metadata.name}')

oc get pod $POD -n $NAMESPACE \
  -o jsonpath='{.metadata.annotations.openshift\.io/scc}{"\n"}'
# → restricted-v2  ✅

# C — Ver el UID arbitrario asignado por OCP
oc exec $POD -n $NAMESPACE -- id
# → uid=1000780000 gid=0(root) — NO es root  ✅

# D — Verificar filesystem readonly
oc exec $POD -n $NAMESPACE -- touch /test 2>&1
# → Read-only file system  ✅

oc exec $POD -n $NAMESPACE -- touch /tmp/test
# → OK (emptyDir en /tmp)  ✅

# E — Ver el rango UID del namespace
oc get namespace $NAMESPACE \
  -o jsonpath='{.metadata.annotations.openshift\.io/sa\.scc\.uid-range}{"\n"}'


# ============================================================
#  ESTADO FINAL
# ============================================================
echo "=== Deployments ==="
oc get deployment -n $NAMESPACE -l app=tienda-api

echo "=== Pods + SCC ==="
oc get pods -n $NAMESPACE -l app=tienda-api \
  -o custom-columns='NAME:.metadata.name,SCC:.metadata.annotations.openshift\.io/scc'

echo "=== HPA ==="
oc get hpa tienda-api-hpa -n $NAMESPACE

echo "=== PDB ==="
oc get pdb tienda-api-pdb -n $NAMESPACE

echo "=== Route ==="
oc get route tienda-api -n $NAMESPACE \
  -o jsonpath='blue={.spec.to.weight} green={.spec.alternateBackends[0].weight}{"\n"}'
