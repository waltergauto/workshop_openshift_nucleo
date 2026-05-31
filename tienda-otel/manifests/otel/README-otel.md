# Stack OTel + Tempo — Instrucciones de despliegue

Complemento de `README.md` principal. Esta guía cubre la instalación
del stack de observabilidad: OBC → Tempo → OTelCollector → apps.

---

## Arquitectura de trazas

```
tienda-otel (namespace)
  ┌─────────────┐   OTLP/gRPC   ┌──────────────────────────────────────┐
  │  pedido-api │ ─────────────► │  OTelCollector (otel-collector)      │
  │ producto-api│               │  ns: openshift-opentelemetry-operator │
  │notificacion │               └──────────────┬───────────────────────┘
  └─────────────┘                              │ OTLP/HTTP (4318)
                                               ▼
                                  ┌────────────────────────┐
                                  │  TempoStack (workshop) │
                                  │  Distributor  :4318    │
                                  │  Ingester             │
                                  │  Querier              │
                                  │  Query Frontend       │
                                  │  Jaeger UI  (Route)   │
                                  └────────────┬───────────┘
                                               │
                                  ┌────────────▼───────────┐
                                  │  ODF/NooBaa S3 bucket  │
                                  │  "tempo-traces-XXXXX"  │
                                  └────────────────────────┘
```

---

## Orden de aplicación

```
05 → 06 → 07 → 08 → 09 → 10
OBC   Job  Tempo Coll  Net  Apps
```

---

## Paso 1 — Crear el bucket S3 (OBC)

```bash
oc apply -f manifests/otel/05-obc-tempo.yaml

# Esperar estado Bound (tarda ~30s)
watch oc get obc tempo-s3-bucket -n openshift-opentelemetry-operator
```

Cuando veas `Bound`, continuar.

---

## Paso 2 — Construir el Secret de credenciales S3

```bash
oc apply -f manifests/otel/06-tempo-s3-secret-job.yaml

# Seguir el Job
oc logs -f job/tempo-s3-secret-builder -n openshift-opentelemetry-operator
```

Verificar que el Secret existe:

```bash
oc get secret tempo-s3-credentials -n openshift-opentelemetry-operator \
  -o jsonpath='{.data}' | python3 -c "
import sys, json, base64
d = json.load(sys.stdin)
for k,v in d.items():
    print(f'{k}: {base64.b64decode(v).decode()}')
"
```

---

## Paso 3 — Desplegar TempoStack

```bash
oc apply -f manifests/otel/07-tempostack.yaml

# Esperar que todos los componentes estén Running (~2-3 min)
watch oc get pods -n openshift-opentelemetry-operator -l app.kubernetes.io/instance=workshop
```

Pods esperados:

```
tempo-workshop-compactor-xxx        1/1 Running
tempo-workshop-distributor-xxx      1/1 Running
tempo-workshop-ingester-0           1/1 Running
tempo-workshop-querier-xxx          1/1 Running
tempo-workshop-query-frontend-xxx   1/1 Running
```

Verificar la Route de Jaeger UI:

```bash
oc get route -n openshift-opentelemetry-operator | grep tempo
```

---

## Paso 4 — Desplegar el OTelCollector

```bash
oc apply -f manifests/otel/08-otel-collector.yaml

# El operador crea el Deployment "otel-collector"
oc rollout status deployment/otel-collector \
  -n openshift-opentelemetry-operator
```

Verificar que el Service del collector existe:

```bash
oc get svc -n openshift-opentelemetry-operator | grep otel
# Debe aparecer: otel-collector-collector   ClusterIP   ...   4317/TCP,4318/TCP
```

> **Nombre del Service:** el operador OTel genera el Service con el
> sufijo `-collector`, por eso el FQDN es:
> `otel-collector-collector.openshift-opentelemetry-operator.svc.cluster.local`

---

## Paso 5 — Aplicar NetworkPolicies

```bash
oc apply -f manifests/otel/09-networkpolicy.yaml
```

---

## Paso 6 — Actualizar las apps con el endpoint correcto

```bash
# Opción rápida con oc set env (recomendada en el workshop)
COLLECTOR="http://otel-collector-collector.openshift-opentelemetry-operator.svc.cluster.local:4317"

for app in producto-api pedido-api notificacion-api; do
  oc set env deployment/$app \
    OTEL_EXPORTER_OTLP_ENDPOINT="$COLLECTOR" \
    -n tienda-otel
done

# O aplicar el ConfigMap + Deployments actualizados
oc apply -f manifests/otel/10-patch-apps-otel-endpoint.yaml
```

Esperar rollout:

```bash
for app in producto-api pedido-api notificacion-api; do
  oc rollout status deployment/$app -n tienda-otel
done
```

---

## Verificación end-to-end

### 1. Ver logs del collector en tiempo real

```bash
oc logs -f deployment/otel-collector-collector \
  -n openshift-opentelemetry-operator
```

### 2. Generar una traza

```bash
# Crear producto de prueba
oc run curl-test --image=curlimages/curl -it --rm --restart=Never \
  -n tienda-otel -- \
  curl -s -X POST http://producto-api:8080/productos \
    -H "Content-Type: application/json" \
    -d '{"nombre":"Laptop Workshop","descripcion":"Demo OTel","precio":1200.00,"stock":50}'

# Crear pedido (genera árbol de 4 spans)
oc run curl-test --image=curlimages/curl -it --rm --restart=Never \
  -n tienda-otel -- \
  curl -s -X POST http://pedido-api:8080/pedidos \
    -H "Content-Type: application/json" \
    -d '{"productoId":1,"cantidad":3,"clienteNombre":"Participante Workshop"}'
```

### 3. Ver la traza en Jaeger UI

```bash
# Obtener URL de la Jaeger UI
oc get route -n openshift-opentelemetry-operator \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.host}{"\n"}{end}'
```

Abrir en el browser → seleccionar servicio `pedido-api` → buscar.

Debés ver un árbol como este:

```
POST /pedidos                        pedido-api         ~150ms
  ├─ pedido.crear                    pedido-api
  ├─ GET /productos/1                producto-api        ~10ms
  ├─ PUT /productos/1/stock          producto-api        ~10ms
  │    └─ producto.actualizar-stock  producto-api
  └─ POST /notificaciones            notificacion-api    ~10ms
       └─ notificacion.registrar     notificacion-api
```

### 4. Probar el caso de error (stock insuficiente)

```bash
oc run curl-test --image=curlimages/curl -it --rm --restart=Never \
  -n tienda-otel -- \
  curl -s -X POST http://pedido-api:8080/pedidos \
    -H "Content-Type: application/json" \
    -d '{"productoId":1,"cantidad":9999,"clienteNombre":"Test Error"}'
```

En Jaeger verás la traza con el span `pedido.crear` marcado en rojo
(`StatusCode = ERROR`) y el atributo `error: Stock insuficiente`.

---

## Troubleshooting

### El collector no recibe trazas

```bash
# Ver si el pod del collector está corriendo
oc get pods -n openshift-opentelemetry-operator | grep otel-collector

# Ver eventos por si hay error de configuración
oc describe otelcollector otel -n openshift-opentelemetry-operator

# Verificar que el Service tiene los puertos correctos
oc get svc otel-collector-collector \
  -n openshift-opentelemetry-operator -o yaml | grep -A5 ports
```

### Las apps no se conectan al collector

```bash
# Test de conectividad desde una app hacia el collector
oc run nc-test --image=registry.access.redhat.com/ubi9/ubi-minimal \
  -it --rm --restart=Never -n tienda-otel -- \
  bash -c "curl -s http://otel-collector-collector.openshift-opentelemetry-operator.svc.cluster.local:4318/v1/traces -o /dev/null -w '%{http_code}'"
# Debe responder 405 (método no permitido, pero conecta)
```

### Tempo no recibe del collector

```bash
# Verificar el endpoint del Distributor
oc get svc -n openshift-opentelemetry-operator | grep distributor

# Ver logs del Distributor de Tempo
oc logs -l app.kubernetes.io/component=distributor \
  -n openshift-opentelemetry-operator --tail=50
```

### El OBC no llega a Bound

```bash
# Ver eventos del OBC
oc describe obc tempo-s3-bucket -n openshift-opentelemetry-operator

# Verificar que el StorageClass de NooBaa existe
oc get storageclass | grep noobaa
# Si no aparece, probar con:
#   storageClassName: ocs-storagecluster-ceph-rgw
# en el archivo 05-obc-tempo.yaml
```

---

## Recursos creados (resumen)

| Recurso | Namespace | Descripción |
|---------|-----------|-------------|
| ObjectBucketClaim `tempo-s3-bucket` | openshift-opentelemetry-operator | Bucket S3 en ODF |
| Secret `tempo-s3-credentials` | openshift-opentelemetry-operator | Credenciales S3 para Tempo |
| TempoStack `workshop` | openshift-opentelemetry-operator | Stack completo de Tempo |
| OpenTelemetryCollector `otel` | openshift-opentelemetry-operator | Collector centralizado |
| NetworkPolicy `allow-tienda-otel-ingress` | openshift-opentelemetry-operator | Tráfico cross-namespace |
| NetworkPolicy `allow-otel-to-tempo` | openshift-opentelemetry-operator | Collector → Tempo |
| ConfigMap `otel-config` | tienda-otel | FQDN del collector para las apps |
| Route (auto) Jaeger UI | openshift-opentelemetry-operator | UI de trazas |
