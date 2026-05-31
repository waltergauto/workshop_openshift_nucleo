# gunicorn.conf.py — Configuración para producción en OpenShift
#
# Ejecutar con:
#   gunicorn -c gunicorn.conf.py app:app

import os
import multiprocessing

# ── Workers ──────────────────────────────────────────────────────────────────
# Fórmula estándar: (2 x CPUs) + 1
# En contenedores se recomienda limitar a 2-4 para evitar OOM
workers     = int(os.environ.get("GUNICORN_WORKERS",
                  min(multiprocessing.cpu_count() * 2 + 1, 4)))
worker_class = "sync"
threads      = 1

# ── Red ──────────────────────────────────────────────────────────────────────
bind        = f"0.0.0.0:{os.environ.get('PORT', '8080')}"
timeout     = 30
keepalive   = 5

# ── Logging ──────────────────────────────────────────────────────────────────
# Gunicorn escribe su access log en stdout y error log en stderr.
# El formato JSON permite que OpenShift/EFK los indexe sin parsers especiales.
accesslog  = "-"   # stdout
errorlog   = "-"   # stderr (Gunicorn usa stderr para sus propios errores)
loglevel   = os.environ.get("LOG_LEVEL", "info").lower()

# Access log en JSON — cada campo es indexable en Kibana/Loki
access_log_format = json_access = (
    '{"timestamp":"%(t)s",'
    '"remote":"%({X-Forwarded-For}i)s",'
    '"method":"%(m)s",'
    '"path":"%(U)s",'
    '"query":"%(q)s",'
    '"status":%(s)s,'
    '"bytes":%(b)s,'
    '"latency_ms":%(D)s,'
    '"correlation_id":"%({X-Correlation-ID}i)s",'
    '"user_agent":"%(a)s"}'
)

# ── Proceso ──────────────────────────────────────────────────────────────────
# En OpenShift el PID 1 es el proceso principal; preload ahorra memoria
preload_app = True
forwarded_allow_ips = "*"   # confiar en X-Forwarded-For del HAProxy de OCP
