package com.workshop.tienda.resource;

import com.workshop.tienda.model.Producto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoResource {

    static final String VERSION = "v2";
    static final String COLOR   = "green";

    static final List<Producto> CATALOGO = List.of(
        new Producto(1,  "Laptop Pro",         1200.00, 15,  "computacion"),
        new Producto(2,  "Monitor 4K",          450.00, 30,  "monitores"),
        new Producto(3,  "Teclado Mecánico",     89.99, 50,  "perifericos"),
        new Producto(4,  "Mouse Inalámbrico",    35.00, 80,  "perifericos"),
        new Producto(5,  "Headset USB",          65.00, 25,  "audio"),
        new Producto(6,  "Webcam HD",            55.00, 40,  "video"),
        new Producto(7,  "Hub USB-C",            29.99, 60,  "accesorios"),
        new Producto(8,  "SSD 1TB",              95.00, 20,  "almacenamiento"),
        new Producto(9,  "RAM 32GB",            110.00, 18,  "computacion"),
        new Producto(10, "Mousepad XL",          18.00, 100, "accesorios")
    );

    // ── GET / ────────────────────────────────────────────────────────────────
    @GET
    public Map<String, Object> index() {
        return Map.of(
            "app",       "tienda-api",
            "version",   VERSION,
            "color",     COLOR,
            "mensaje",   "TiendaAPI v2 — paginación y filtro por categoría",
            "endpoints", List.of(
                "/productos",
                "/productos?page=1&limit=5",
                "/productos?categoria=perifericos",
                "/version", "/health", "/stress"
            )
        );
    }

    // ── GET /productos ───────────────────────────────────────────────────────
    @GET
    @Path("/productos")
    public Map<String, Object> productos(
            @QueryParam("page")      @DefaultValue("1")  int    page,
            @QueryParam("limit")     @DefaultValue("10") int    limit,
            @QueryParam("categoria")                     String categoria) {

        // Filtro opcional por categoría
        List<Producto> filtrado = (categoria != null && !categoria.isBlank())
            ? CATALOGO.stream().filter(p -> p.categoria.equalsIgnoreCase(categoria)).toList()
            : CATALOGO;

        int total = filtrado.size();
        int start = (page - 1) * limit;
        int end   = Math.min(start + limit, total);
        int pages = (int) Math.ceil((double) total / limit);

        List<Producto> pagina = (start < total) ? filtrado.subList(start, end) : List.of();

        // LinkedHashMap para mantener orden de campos en el JSON
        var result = new LinkedHashMap<String, Object>();
        result.put("version",   VERSION);
        result.put("total",     total);
        result.put("page",      page);
        result.put("limit",     limit);
        result.put("pages",     pages);
        result.put("productos", pagina);
        result.put("_meta", Map.of(
            "has_next", end < total,
            "has_prev", page > 1
        ));
        return result;
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
    public Map<String, Object> version() {
        return Map.of(
            "version",         VERSION,
            "color",           COLOR,
            "host",            System.getenv().getOrDefault("HOSTNAME", "local"),
            "nuevas_features", List.of("paginacion", "filtro_categoria")
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
            for (int i = 1; i <= 10_000; i++) result += i;
        }
        return Map.of(
            "version",  VERSION,
            "stressed", true,
            "duration", duration,
            "host",     System.getenv().getOrDefault("HOSTNAME", "local"),
            "checksum", result
        );
    }
}
