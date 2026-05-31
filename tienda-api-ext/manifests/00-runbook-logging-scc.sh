#!/usr/bin/env bash
# ============================================================
#  RUNBOOK — Módulos: Logging + Contenedores Rootless
#  OpenShift 4.18 | tienda-api v3
# ============================================================

NAMESPACE="tienda-api-workshop"
ROUTE=$(oc get route tienda-api -n $NAMESPACE -o jsonpath='{.spec.host}' 2>/dev/null)


# ============================================================
#  MÓDULO A — LOGGING DE APLICACIONES
# ============================================================

# ------------------------------------------------------------
# A.1 — stdout vs stderr: verlos por separado con oc logs
# ------------------------------------------------------------

# Todos los logs del pod (stdout + stderr mezclados)
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE

# Solo stdout  (INFO, WARNING, DEBUG)
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE 2>/dev/null

# Solo stderr  (ERROR, CRITICAL)
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE 1>/dev/null

# Seguir los logs en tiempo real (como tail -f)
oc logs -f -l app=tienda-api,version=v3 -n $NAMESPACE

# Logs de todos los pods del Deployment (muy útil con HPA activo)
oc logs deployment/tienda-api-v3 -n $NAMESPACE --all-containers

# Últimas N líneas
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE --tail=50

# Logs desde hace X tiempo
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE --since=5m


# ------------------------------------------------------------
# A.2 — Generar distintos niveles de log para el ejercicio
# ------------------------------------------------------------

# INFO: request normal
curl -sk https://$ROUTE/productos

# WARNING: producto inexistente (404)
curl -sk https://$ROUTE/productos/9999

# ERROR: endpoint que lanza excepción no capturada
curl -sk https://$ROUTE/error-demo

# Verificar que el ERROR fue a stderr:
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE 1>/dev/null


# ------------------------------------------------------------
# A.3 — Correlación de requests: rastrear un request específico
# ------------------------------------------------------------

# Enviar un request con Correlation ID propio
CORR_ID="workshop-demo-$(date +%s)"
curl -sk -H "X-Correlation-ID: $CORR_ID" https://$ROUTE/productos

# Buscar ese ID en los logs (filtra solo líneas relevantes)
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE | \
  grep "$CORR_ID"

# Ver el campo en formato legible (requiere jq)
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE | \
  grep "$CORR_ID" | \
  jq '{time: .timestamp, msg: .message, latency: .latency_ms, status: .status}'


# ------------------------------------------------------------
# A.4 — Parsear logs JSON con jq (ejercicio en vivo)
# ------------------------------------------------------------

# Todos los requests con status 4xx o 5xx
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE | \
  jq 'select(.status >= 400)' 2>/dev/null

# Promedio de latencia de todos los requests
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE | \
  jq -s '[.[] | select(.latency_ms != null) | .latency_ms] | add/length' 2>/dev/null

# Agrupar por mensaje de evento
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE | \
  jq -s 'group_by(.message) | map({event: .[0].message, count: length})' 2>/dev/null

# Solo los ERRORs con su traceback
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE | \
  jq 'select(.level == "ERROR") | {time: .timestamp, error: .error, trace: .traceback}' 2>/dev/null


# ------------------------------------------------------------
# A.5 — Logs de pods anteriores (pod reiniciado / crasheado)
# ------------------------------------------------------------
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE --previous


# ============================================================
#  MÓDULO B — CONTENEDORES ROOTLESS + SCC
# ============================================================

# ------------------------------------------------------------
# B.1 — Demostrar el problema: imagen que corre como root
# ------------------------------------------------------------

# Desplegar la imagen "mala" (Dockerfile.bad)
oc create deployment tienda-api-nosec \
  --image=tienda-api:v3-bad \
  --replicas=1 \
  -n $NAMESPACE

# Observar el error
oc get pods -l app=tienda-api-nosec -n $NAMESPACE
oc describe pod -l app=tienda-api-nosec -n $NAMESPACE | grep -A5 "Events"
# → Error: container has runAsNonRoot and image will run as root

# Ver qué SCC intentó usar
oc get pod -l app=tienda-api-nosec -n $NAMESPACE \
  -o jsonpath='{.items[0].metadata.annotations.openshift\.io/scc}'

# Limpiar
oc delete deployment tienda-api-nosec -n $NAMESPACE


# ------------------------------------------------------------
# B.2 — Desplegar la imagen OCP-ready (Dockerfile.good)
# ------------------------------------------------------------
oc apply -f manifests/06-scc-security.yaml -n $NAMESPACE
oc rollout status deployment/tienda-api-v3 -n $NAMESPACE

# Ver qué SCC asignó OpenShift
POD=$(oc get pods -l app=tienda-api,version=v3 -n $NAMESPACE \
      -o jsonpath='{.items[0].metadata.name}')

oc get pod $POD -n $NAMESPACE \
  -o jsonpath='{.metadata.annotations.openshift\.io/scc}'
# → restricted-v2  ✅


# ------------------------------------------------------------
# B.3 — Inspeccionar el UID arbitrario asignado por OCP
# ------------------------------------------------------------

# Ver con qué UID corre el proceso dentro del contenedor
oc exec $POD -n $NAMESPACE -- id
# → uid=1000780000(1000780000) gid=0(root) groups=0(root)
#   UID es un número grande aleatorio del rango del namespace → ✅ no es 0

# Verificar que el filesystem es readonly (excepto /tmp)
oc exec $POD -n $NAMESPACE -- touch /test-file 2>&1
# → touch: cannot touch '/test-file': Read-only file system  ✅

oc exec $POD -n $NAMESPACE -- touch /tmp/test-file
# → funciona (emptyDir montado en /tmp)  ✅


# ------------------------------------------------------------
# B.4 — Ver el rango de UIDs del namespace
# ------------------------------------------------------------
oc get namespace $NAMESPACE \
  -o jsonpath='{.metadata.annotations.openshift\.io/sa\.scc\.uid-range}'
# → 1000780000/10000  (empieza en ese UID, rango de 10000)


# ------------------------------------------------------------
# B.5 — Ver todos los SCCs disponibles en el cluster
# ------------------------------------------------------------
oc get scc
# SCCs de menor a mayor permisividad:
#   restricted-v2    → default para pods sin ServiceAccount especial
#   restricted        → versión legacy
#   baseline          → algo más permisivo
#   nonroot           → permite cualquier UID != 0
#   anyuid            → permite cualquier UID incluyendo root (peligroso)
#   privileged        → acceso completo al nodo (solo para infra)


# ------------------------------------------------------------
# B.6 — Verificar qué SCC puede usar un ServiceAccount
# ------------------------------------------------------------
oc adm policy who-can use scc restricted-v2 -n $NAMESPACE

# Ver los SCCs disponibles para el SA default del namespace
oc adm policy review \
  -z default \
  -n $NAMESPACE \
  --resource=pods


# ------------------------------------------------------------
# B.7 — Otorgar SCC personalizado a un ServiceAccount (si fuera necesario)
# ------------------------------------------------------------
# Crear el SA (ya está en el manifest)
oc apply -f manifests/06-scc-security.yaml -n $NAMESPACE

# Vincular el SCC al SA (requiere cluster-admin)
oc adm policy add-scc-to-user tienda-api-scc \
  -z tienda-api-sa \
  -n $NAMESPACE

# Actualizar el Deployment para usar el SA
oc patch deployment tienda-api-v3 -n $NAMESPACE \
  -p '{"spec":{"template":{"spec":{"serviceAccountName":"tienda-api-sa"}}}}'


# ------------------------------------------------------------
# B.8 — Checklist imagen OCP-ready (resumen para el workshop)
# ------------------------------------------------------------
cat <<'CHECKLIST'

  CHECKLIST: Imagen lista para OpenShift
  ═══════════════════════════════════════
  [✓] Base image: UBI9 o imagen certificada Red Hat
  [✓] USER 1001 (o cualquier UID != 0) en el Dockerfile
  [✓] Puerto >= 1024 (no privilegiado)
  [✓] chown -R 1001:0 + chmod -R g=u en directorios de trabajo
  [✓] PYTHONUNBUFFERED=1 (o equivalente según lenguaje)
  [✓] readOnlyRootFilesystem: true + emptyDir en /tmp
  [✓] allowPrivilegeEscalation: false
  [✓] capabilities.drop: [ALL]
  [✓] Sin secretos hardcodeados en la imagen
  [✓] Sin herramientas de debug (curl, wget, bash) en producción

CHECKLIST


# ============================================================
#  ESTADO FINAL DEL NAMESPACE
# ============================================================
echo "=== Deployments ==="
oc get deployment -n $NAMESPACE -l app=tienda-api

echo "=== Pods y sus SCCs ==="
oc get pods -n $NAMESPACE -l app=tienda-api \
  -o custom-columns='NAME:.metadata.name,SCC:.metadata.annotations.openshift\.io/scc,NODE:.spec.nodeName'

echo "=== Logs recientes (últimas 5 líneas JSON) ==="
oc logs -l app=tienda-api,version=v3 -n $NAMESPACE --tail=5
