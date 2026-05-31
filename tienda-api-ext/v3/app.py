"""
tienda-api v3
=============
Módulo: Logging estructurado (JSON) + Correlación de requests

Novedades respecto a v2:
  - Logging en formato JSON (listo para OpenShift/EFK/Loki)
  - Correlation ID propagado en cada request (X-Correlation-ID)
  - Log de acceso por request (método, path, status, latencia)
  - Log de error estructurado con traceback
  - Separación correcta stdout (INFO/DEBUG) vs stderr (ERROR/CRITICAL)
"""

import os
import sys
import time
import math
import uuid
import logging
import traceback

from flask import Flask, jsonify, request, g

# ── Importar el módulo de logging estructurado ──────────────────────────────
from logger import get_logger, JsonFormatter

app = Flask(__name__)
log = get_logger(__name__)

APP_VERSION = "v3"
APP_COLOR   = "green"

PRODUCTOS = [
    {"id": 1, "nombre": "Laptop Pro",        "precio": 1200.00, "stock": 15, "categoria": "computacion"},
    {"id": 2, "nombre": "Monitor 4K",         "precio":  450.00, "stock": 30, "categoria": "monitores"},
    {"id": 3, "nombre": "Teclado Mecánico",   "precio":   89.99, "stock": 50, "categoria": "perifericos"},
    {"id": 4, "nombre": "Mouse Inalámbrico",  "precio":   35.00, "stock": 80, "categoria": "perifericos"},
    {"id": 5, "nombre": "Headset USB",        "precio":   65.00, "stock": 25, "categoria": "audio"},
    {"id": 6, "nombre": "Webcam HD",          "precio":   55.00, "stock": 40, "categoria": "video"},
    {"id": 7, "nombre": "Hub USB-C",          "precio":   29.99, "stock": 60, "categoria": "accesorios"},
    {"id": 8, "nombre": "SSD 1TB",            "precio":   95.00, "stock": 20, "categoria": "almacenamiento"},
    {"id": 9, "nombre": "RAM 32GB",           "precio":  110.00, "stock": 18, "categoria": "computacion"},
    {"id":10, "nombre": "Mousepad XL",        "precio":   18.00, "stock": 100,"categoria": "accesorios"},
]


# ── Middleware: asignar/propagar Correlation ID ──────────────────────────────
@app.before_request
def before_request():
    # Reusar el ID que viene del cliente (API Gateway, otro servicio)
    # o generar uno nuevo si no viene
    correlation_id = request.headers.get("X-Correlation-ID", str(uuid.uuid4()))
    g.correlation_id = correlation_id
    g.start_time = time.time()

    log.info("request_started", extra={
        "correlation_id": correlation_id,
        "method":  request.method,
        "path":    request.path,
        "remote":  request.remote_addr,
    })


@app.after_request
def after_request(response):
    latency_ms = round((time.time() - g.start_time) * 1000, 2)

    log.info("request_finished", extra={
        "correlation_id": g.correlation_id,
        "method":         request.method,
        "path":           request.path,
        "status":         response.status_code,
        "latency_ms":     latency_ms,
    })

    # Devolver el correlation ID al cliente para trazabilidad
    response.headers["X-Correlation-ID"] = g.correlation_id
    return response


# ── Manejador global de errores no capturados ────────────────────────────────
@app.errorhandler(Exception)
def handle_exception(e):
    log.error("unhandled_exception", extra={
        "correlation_id": getattr(g, "correlation_id", "n/a"),
        "error":          str(e),
        "traceback":      traceback.format_exc(),
    })
    return jsonify({"error": "Error interno del servidor",
                    "correlation_id": getattr(g, "correlation_id", "n/a")}), 500


# ── Endpoints ────────────────────────────────────────────────────────────────
@app.route("/")
def index():
    return jsonify({
        "app":     "tienda-api",
        "version": APP_VERSION,
        "color":   APP_COLOR,
        "mensaje": "TiendaAPI v3 — logging estructurado + correlation ID",
    })


@app.route("/productos")
def productos():
    page      = int(request.args.get("page",  1))
    limit     = int(request.args.get("limit", 10))
    categoria = request.args.get("categoria")

    resultado = PRODUCTOS
    if categoria:
        resultado = [p for p in PRODUCTOS if p["categoria"] == categoria]
        log.info("productos_filtrados", extra={
            "correlation_id": g.correlation_id,
            "categoria":      categoria,
            "total":          len(resultado),
        })

    total  = len(resultado)
    start  = (page - 1) * limit
    pagina = resultado[start:start + limit]

    return jsonify({
        "version":   APP_VERSION,
        "total":     total,
        "page":      page,
        "limit":     limit,
        "pages":     math.ceil(total / limit) if limit else 1,
        "productos": pagina,
    })


@app.route("/productos/<int:producto_id>")
def producto_detalle(producto_id):
    producto = next((p for p in PRODUCTOS if p["id"] == producto_id), None)
    if not producto:
        log.warning("producto_not_found", extra={
            "correlation_id": g.correlation_id,
            "producto_id":    producto_id,
        })
        return jsonify({"error": "Producto no encontrado",
                        "correlation_id": g.correlation_id}), 404
    return jsonify({"version": APP_VERSION, "producto": producto})


@app.route("/version")
def version():
    return jsonify({
        "version": APP_VERSION,
        "color":   APP_COLOR,
        "host":    os.environ.get("HOSTNAME", "unknown"),
    })


@app.route("/health")
def health():
    return jsonify({"status": "ok", "version": APP_VERSION}), 200


@app.route("/stress")
def stress():
    seconds = min(int(request.args.get("seconds", 5)), 30)
    log.info("stress_start", extra={
        "correlation_id": g.correlation_id,
        "duration":       seconds,
    })
    start = time.time()
    while time.time() - start < seconds:
        math.factorial(10000)
    return jsonify({"version": APP_VERSION, "stressed": True, "duration": seconds})


@app.route("/error-demo")
def error_demo():
    """Endpoint para demostrar logging de errores en el workshop."""
    raise ValueError("Error de demostración — revisa los logs en OpenShift")


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    log.info("app_startup", extra={
        "version": APP_VERSION,
        "port":    port,
        "host":    os.environ.get("HOSTNAME", "unknown"),
    })
    app.run(host="0.0.0.0", port=port, debug=False)
