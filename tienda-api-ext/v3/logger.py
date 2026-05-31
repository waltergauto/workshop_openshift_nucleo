"""
logger.py — Logging estructurado en JSON para OpenShift/Kubernetes
===================================================================

Diseño:
  • INFO / DEBUG / WARNING  → stdout  (oc logs los muestra normalmente)
  • ERROR / CRITICAL        → stderr  (OpenShift marca estos en rojo en la consola)
  • Formato JSON con campos estándar para EFK / Loki / Splunk

Campos incluidos en cada línea de log:
  timestamp     ISO-8601 UTC
  level         INFO | WARNING | ERROR | CRITICAL | DEBUG
  logger        nombre del módulo que emitió el log
  message       texto del evento (snake_case recomendado)
  app           nombre de la aplicación
  version       versión de la app (de env APP_VERSION)
  host          nombre del pod (HOSTNAME)
  + cualquier campo extra pasado en el parámetro extra={}
"""

import os
import sys
import json
import logging
from datetime import datetime, timezone


APP_NAME    = os.environ.get("APP_NAME",    "tienda-api")
APP_VERSION = os.environ.get("APP_VERSION", "v3")
HOSTNAME    = os.environ.get("HOSTNAME",    "local")
LOG_LEVEL   = os.environ.get("LOG_LEVEL",  "INFO").upper()


class JsonFormatter(logging.Formatter):
    """Formatea cada LogRecord como una línea JSON."""

    # Campos que ya están en LogRecord y no queremos duplicar en "extra"
    _SKIP = {
        "name", "msg", "args", "levelname", "levelno", "pathname",
        "filename", "module", "exc_info", "exc_text", "stack_info",
        "lineno", "funcName", "created", "msecs", "relativeCreated",
        "thread", "threadName", "processName", "process", "message",
        "taskName",
    }

    def format(self, record: logging.LogRecord) -> str:
        # Mensaje principal
        record.message = record.getMessage()

        entry = {
            "timestamp": datetime.fromtimestamp(
                record.created, tz=timezone.utc
            ).isoformat(),
            "level":   record.levelname,
            "logger":  record.name,
            "message": record.message,
            "app":     APP_NAME,
            "version": APP_VERSION,
            "host":    HOSTNAME,
        }

        # Agregar campos extra (correlation_id, latency_ms, etc.)
        for key, value in record.__dict__.items():
            if key not in self._SKIP and not key.startswith("_"):
                entry[key] = value

        # Incluir traceback si hay excepción
        if record.exc_info:
            entry["exception"] = self.formatException(record.exc_info)

        return json.dumps(entry, ensure_ascii=False)


class StdoutFilter(logging.Filter):
    """Permite solo niveles < ERROR en stdout."""
    def filter(self, record):
        return record.levelno < logging.ERROR


def get_logger(name: str) -> logging.Logger:
    """
    Devuelve un logger configurado con:
      - Handler stdout → INFO/DEBUG/WARNING
      - Handler stderr → ERROR/CRITICAL
    """
    logger = logging.getLogger(name)

    if logger.handlers:
        return logger  # ya configurado (evitar duplicados en Gunicorn)

    logger.setLevel(getattr(logging, LOG_LEVEL, logging.INFO))
    formatter = JsonFormatter()

    # ── stdout: INFO, DEBUG, WARNING ─────────────────────────────────────
    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setFormatter(formatter)
    stdout_handler.addFilter(StdoutFilter())

    # ── stderr: ERROR, CRITICAL ──────────────────────────────────────────
    stderr_handler = logging.StreamHandler(sys.stderr)
    stderr_handler.setFormatter(formatter)
    stderr_handler.setLevel(logging.ERROR)

    logger.addHandler(stdout_handler)
    logger.addHandler(stderr_handler)
    logger.propagate = False

    return logger
