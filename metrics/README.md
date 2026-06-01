metrics-app (namespace)
  └── metrics-demo (Quarkus + Micrometer)
        └── GET /q/metrics  ← endpoint Prometheus

openshift-monitoring (namespace)
  └── Prometheus (User Workload)
        └── ServiceMonitor → scrape metrics-demo cada 30s
        └── PrometheusRule → alertas (ej: tasa de errores alta)
  └── Alertmanager
        └── recibe alertas del PrometheusRule


Pruebas
APP=$(oc get route metrics-demo -n metrics-app -o jsonpath='{.spec.host}')

# Counter — éxitos y errores
curl https://$APP/api/contador
curl https://$APP/api/contador
curl "https://$APP/api/contador?error=true"

# Gauge — usuarios entrando y saliendo
curl "https://$APP/api/gauge?accion=entrar"
curl "https://$APP/api/gauge?accion=entrar"
curl "https://$APP/api/gauge?accion=salir"

# Timer — operación de 500ms
curl "https://$APP/api/timer?ms=500"

# Disparar alerta de errores (llamar varias veces)
for i in {1..10}; do curl https://$APP/api/error; done

# Ver el endpoint de métricas raw
curl https://$APP/q/metrics | grep workshop
