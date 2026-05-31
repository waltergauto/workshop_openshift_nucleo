# ============================================================
#  TIENDA-API — RUNBOOK DEL WORKSHOP
#  OpenShift 4.18 | HPA · Blue-Green · Canary · PDB
# ============================================================

# ------------------------------------------------------------
# PASO 0 — Preparación del namespace
# ------------------------------------------------------------
oc new-project tienda-api-workshop

# Dar permisos de anyuid si el cluster lo requiere (lab)
oc adm policy add-scc-to-serviceaccount anyuid -z default -n tienda-api-workshop


# ------------------------------------------------------------
# PASO 1 — Build de imágenes con S2I
# ------------------------------------------------------------
oc apply -f manifests/01-build.yaml

# Disparar builds manualmente (si no hay webhook configurado aún)
oc start-build tienda-api-v1 --follow
oc start-build tienda-api-v2 --follow

# Verificar que las imágenes existen
oc get istag tienda-api:v1 tienda-api:v2


# ------------------------------------------------------------
# PASO 2 — Desplegar baseline (blue / v1)
# ------------------------------------------------------------
oc apply -f manifests/02-deployments.yaml
oc apply -f manifests/03-services-route.yaml

# Esperar que los pods estén Ready
oc rollout status deployment/tienda-api-blue

# Obtener la URL pública
ROUTE=$(oc get route tienda-api -o jsonpath='{.spec.host}')
echo "URL: https://$ROUTE"

# Verificar que responde
curl -sk https://$ROUTE/version
curl -sk https://$ROUTE/productos


# ------------------------------------------------------------
# PASO 3 — PDB: proteger antes de todo lo demás
# ------------------------------------------------------------
oc apply -f manifests/05-pdb.yaml
oc get pdb tienda-api-pdb

# -- EJERCICIO DE BLOQUEO --
# Editar 05-pdb.yaml: cambiar maxUnavailable:1  por  minAvailable:2
# Bajar réplicas a 2 (ya están en 2)
oc scale deployment tienda-api-blue --replicas=2

# Intentar drenar (solo simulación con --dry-run)
NODE=$(oc get pods -l app=tienda-api,version=blue -o jsonpath='{.items[0].spec.nodeName}')
oc adm drain $NODE --pod-selector=app=tienda-api --ignore-daemonsets --dry-run

# Resolver: subir a 3 réplicas
oc scale deployment tienda-api-blue --replicas=3

# Restaurar PDB a maxUnavailable:1 para continuar
oc apply -f manifests/05-pdb.yaml


# ------------------------------------------------------------
# PASO 4 — HPA: manejar pico de carga
# ------------------------------------------------------------
oc apply -f manifests/04-hpa.yaml

# Estado inicial del HPA
oc get hpa tienda-api-hpa

# -- GENERAR CARGA --
# Terminal A: carga sostenida
oc run load-gen \
  --image=registry.access.redhat.com/ubi9/ubi-minimal \
  --restart=Never \
  --rm -it -- \
  sh -c "while true; do curl -sk https://$ROUTE/stress?seconds=2 > /dev/null; done"

# Terminal B: observar el escalado (cada 10 seg)
watch -n10 "oc get hpa tienda-api-hpa && echo && oc get pods -l app=tienda-api,version=blue"

# Detener la carga (Ctrl+C en Terminal A o eliminar el pod)
oc delete pod load-gen --ignore-not-found

# Observar el scale-down lento (stabilizationWindow=120s)
watch -n10 "oc get hpa tienda-api-hpa"


# ------------------------------------------------------------
# PASO 5 — CANARY: 10% del tráfico a v2
# ------------------------------------------------------------
# Verificar que green (v2) está desplegado
oc rollout status deployment/tienda-api-green

# Fase 1: 10% canary
oc set route-backends tienda-api \
  tienda-api-blue=90 tienda-api-green=10

oc get route tienda-api

# Validar la distribución del tráfico
echo "--- Distribución de versiones (30 llamadas) ---"
for i in $(seq 1 30); do
  curl -sk https://$ROUTE/version | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['version'])"
done | sort | uniq -c

# Fase 2: 50/50 (si las métricas son buenas)
oc set route-backends tienda-api \
  tienda-api-blue=50 tienda-api-green=50

# Validar de nuevo
for i in $(seq 1 20); do
  curl -sk https://$ROUTE/version | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['version'])"
done | sort | uniq -c

# Fase 3: 100% green
oc set route-backends tienda-api \
  tienda-api-blue=0 tienda-api-green=100


# ------------------------------------------------------------
# PASO 6 — BLUE-GREEN: switch definitivo + rollback
# ------------------------------------------------------------
# Switch final: todo el tráfico a green
# (ya está en 100% desde el paso anterior, solo confirmamos)
curl -sk https://$ROUTE/version
# → debe responder v2

# -- ROLLBACK de emergencia (tarda < 1 segundo) --
oc set route-backends tienda-api \
  tienda-api-blue=100 tienda-api-green=0

curl -sk https://$ROUTE/version
# → vuelve a v1

# Switch definitivo de nuevo a v2
oc set route-backends tienda-api \
  tienda-api-blue=0 tienda-api-green=100

# Escalar green a 3 réplicas para igualar capacidad
oc scale deployment tienda-api-green --replicas=3
oc rollout status deployment/tienda-api-green

# Apagar blue con cero réplicas (no eliminar, por si acaso)
oc scale deployment tienda-api-blue --replicas=0

# Estado final
oc get deployment,hpa,pdb,route -l app=tienda-api


# ------------------------------------------------------------
# LIMPIEZA (al final del workshop)
# ------------------------------------------------------------
oc delete project tienda-api-workshop
