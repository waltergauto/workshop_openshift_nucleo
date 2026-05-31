import os
import time
import math
from flask import Flask, jsonify, request

app = Flask(__name__)

APP_VERSION = "v2"
APP_COLOR   = "green"

PRODUCTOS = [
    {"id": 1, "nombre": "Laptop Pro",       "precio": 1200.00, "stock": 15, "categoria": "computacion"},
    {"id": 2, "nombre": "Monitor 4K",       "precio":  450.00, "stock": 30, "categoria": "monitores"},
    {"id": 3, "nombre": "Teclado Mecánico", "precio":   89.99, "stock": 50, "categoria": "perifericos"},
    {"id": 4, "nombre": "Mouse Inalámbrico","precio":   35.00, "stock": 80, "categoria": "perifericos"},
    {"id": 5, "nombre": "Headset USB",      "precio":   65.00, "stock": 25, "categoria": "audio"},
    {"id": 6, "nombre": "Webcam HD",        "precio":   55.00, "stock": 40, "categoria": "video"},
    {"id": 7, "nombre": "Hub USB-C",        "precio":   29.99, "stock": 60, "categoria": "accesorios"},
    {"id": 8, "nombre": "SSD 1TB",          "precio":   95.00, "stock": 20, "categoria": "almacenamiento"},
    {"id": 9, "nombre": "RAM 32GB",         "precio":  110.00, "stock": 18, "categoria": "computacion"},
    {"id":10, "nombre": "Mousepad XL",      "precio":   18.00, "stock": 100,"categoria": "accesorios"},
]


@app.route("/")
def index():
    return jsonify({
        "app":     "tienda-api",
        "version": APP_VERSION,
        "color":   APP_COLOR,
        "mensaje": "Bienvenido a TiendaAPI — ahora con paginación y búsqueda",
        "endpoints": ["/productos", "/productos?page=1&limit=5", "/productos?categoria=perifericos",
                      "/version", "/health", "/stress"]
    })


@app.route("/productos")
def productos():
    # --- paginación (nueva en v2) ---
    page  = int(request.args.get("page",  1))
    limit = int(request.args.get("limit", 10))

    # --- filtro por categoría (nuevo en v2) ---
    categoria = request.args.get("categoria", None)
    resultado = PRODUCTOS
    if categoria:
        resultado = [p for p in PRODUCTOS if p["categoria"] == categoria]

    total = len(resultado)
    start = (page - 1) * limit
    end   = start + limit
    pagina = resultado[start:end]

    return jsonify({
        "version":    APP_VERSION,
        "total":      total,
        "page":       page,
        "limit":      limit,
        "pages":      math.ceil(total / limit) if limit else 1,
        "productos":  pagina,
        "_meta": {
            "has_next": end < total,
            "has_prev": page > 1
        }
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
        "host":    os.environ.get("HOSTNAME", "unknown"),
        "nuevas_features": ["paginacion", "filtro_categoria"]
    })


@app.route("/health")
def health():
    return jsonify({"status": "ok", "version": APP_VERSION}), 200


@app.route("/stress")
def stress():
    """Genera carga de CPU para demostrar el HPA."""
    seconds = int(request.args.get("seconds", 5))
    seconds = min(seconds, 30)

    start = time.time()
    while time.time() - start < seconds:
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
