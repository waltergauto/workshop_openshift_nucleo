package com.workshop.tienda.resource;

import com.workshop.tienda.model.Producto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoResource {

    static final String VERSION = "v1";
    static final String COLOR   = "blue";

    static final List<Producto> CATALOGO = List.of(
        new Producto(1,  "Laptop Pro",         1200.00, 15),
        new Producto(2,  "Monitor 4K",          450.00, 30),
        new Producto(3,  "Teclado Mecánico",     89.99, 50),
        new Producto(4,  "Mouse Inalámbrico",    35.00, 80),
        new Producto(5,  "Headset USB",          65.00, 25),
        new Producto(6,  "Webcam HD",            55.00, 40),
        new Producto(7,  "Hub USB-C",            29.99, 60),
        new Producto(8,  "SSD 1TB",              95.00, 20),
        new Producto(9,  "RAM 32GB",            110.00, 18),
        new Producto(10, "Mousepad XL",          18.00, 100)
    );

    // ── GET / ────────────────────────────────────────────────────────────────
    @GET
    public Map<String, Object> index() {
        return Map.of(
            "app",       "tienda-api",
            "version",   VERSION,
            "color",     COLOR,
            "mensaje",   "Bienvenido a TiendaAPI",
            "endpoints", List.of("/productos", "/version", "/health", "/stress")
        );
    }

    // ── GET /productos ───────────────────────────────────────────────────────
    @GET
    @Path("/productos")
    public Map<String, Object> productos() {
        return Map.of(
            "version",   VERSION,
            "total",     CATALOGO.size(),
            "productos", CATALOGO
        );
    }

    // ── GET /productos/{id} ──────────────────────────────────────────────────
    @GET
    @Path("/productos/{id}")
    public Response productoPorId(@PathParam("id") int id) {
        return CATALOGO.stream()
            .filter(p -> p.id == id)
            .findFirst()
            .map(p -> Response.ok(Map.of("version", VERSION, "producto", p)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Producto no encontrado")).build());
    }

    // ── GET /version ─────────────────────────────────────────────────────────
    @GET
    @Path("/version")
    public Map<String, String> version() {
        return Map.of(
            "version", VERSION,
            "color",   COLOR,
            "host",    System.getenv().getOrDefault("HOSTNAME", "local")
        );
    }

    // ── GET /stress ──────────────────────────────────────────────────────────
    @GET
    @Path("/stress")
    public Map<String, Object> stress(@QueryParam("seconds") @DefaultValue("5") int seconds) {
        int duration = Math.min(seconds, 30);
        long end = System.currentTimeMillis() + (duration * 1000L);
        long result = 0;
        while (System.currentTimeMillis() < end) {
            // operación CPU-intensiva
            for (int i = 1; i <= 10_000; i++) result += i;
        }
        return Map.of(
            "version",  VERSION,
            "stressed", true,
            "duration", duration,
            "host",     System.getenv().getOrDefault("HOSTNAME", "local"),
            "checksum", result  // evita que el JIT elimine el loop
        );
    }
}
