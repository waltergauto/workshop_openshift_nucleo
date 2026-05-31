import os
import time
import math
from flask import Flask, jsonify, request

app = Flask(__name__)

APP_VERSION = "v1"
APP_COLOR   = "blue"

PRODUCTOS = [
    {"id": 1, "nombre": "Laptop Pro",      "precio": 1200.00, "stock": 15},
    {"id": 2, "nombre": "Monitor 4K",      "precio":  450.00, "stock": 30},
    {"id": 3, "nombre": "Teclado Mecánico","precio":   89.99, "stock": 50},
    {"id": 4, "nombre": "Mouse Inalámbrico","precio":  35.00, "stock": 80},
    {"id": 5, "nombre": "Headset USB",     "precio":   65.00, "stock": 25},
    {"id": 6, "nombre": "Webcam HD",       "precio":   55.00, "stock": 40},
    {"id": 7, "nombre": "Hub USB-C",       "precio":   29.99, "stock": 60},
    {"id": 8, "nombre": "SSD 1TB",         "precio":   95.00, "stock": 20},
    {"id": 9, "nombre": "RAM 32GB",        "precio":   110.00,"stock": 18},
    {"id":10, "nombre": "Mousepad XL",     "precio":   18.00, "stock": 100},
]


@app.route("/")
def index():
    return jsonify({
        "app":     "tienda-api",
        "version": APP_VERSION,
        "color":   APP_COLOR,
        "mensaje": "Bienvenido a TiendaAPI",
        "endpoints": ["/productos", "/version", "/health", "/stress"]
    })


@app.route("/productos")
def productos():
    return jsonify({
        "version":   APP_VERSION,
        "total":     len(PRODUCTOS),
        "productos": PRODUCTOS
    })


@app.route("/productos/<int:producto_id>")
def producto_detalle(producto_id):
    producto = next((p for p in PRODUCTOS if p["id"] == producto_id), None)
    if not producto:
        return jsonify({"error": "Producto no encontrado"}), 404
    return jsonify({"version": APP_VERSION, "producto": producto})


@app.route("/version")
def version():
    return jsonify({
        "version": APP_VERSION,
        "color":   APP_COLOR,
        "host":    os.environ.get("HOSTNAME", "unknown")
    })


@app.route("/health")
def health():
    return jsonify({"status": "ok", "version": APP_VERSION}), 200


@app.route("/stress")
def stress():
    """Genera carga de CPU para demostrar el HPA."""
    seconds = int(request.args.get("seconds", 5))
    seconds = min(seconds, 30)  # máximo 30 seg por seguridad

    start = time.time()
    while time.time() - start < seconds:
        # operación CPU-intensiva
        math.factorial(10000)

    return jsonify({
        "version":  APP_VERSION,
        "stressed": True,
        "duration": seconds,
        "host":     os.environ.get("HOSTNAME", "unknown")
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    app.run(host="0.0.0.0", port=port, debug=False)
