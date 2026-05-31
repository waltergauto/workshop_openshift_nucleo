package com.workshop.tienda.resource;

import com.workshop.tienda.model.Producto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoResource {

    private static final Logger LOG = Logger.getLogger(ProductoResource.class);

    static final String VERSION = "v3";
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
            "app",     "tienda-api",
            "version", VERSION,
            "color",   COLOR,
            "mensaje", "TiendaAPI v3 — JSON logging + correlación + rootless"
        );
    }

    // ── GET /productos ───────────────────────────────────────────────────────
    @GET
    @Path("/productos")
    public Map<String, Object> productos(
            @QueryParam("page")      @DefaultValue("1")  int    page,
            @QueryParam("limit")     @DefaultValue("10") int    limit,
            @QueryParam("categoria")                     String categoria) {

        List<Producto> filtrado = (categoria != null && !categoria.isBlank())
            ? CATALOGO.stream().filter(p -> p.categoria.equalsIgnoreCase(categoria)).toList()
            : CATALOGO;

        // Log estructurado: el correlationId viene del MDC automáticamente
        if (categoria != null && !categoria.isBlank()) {
            MDC.put("categoria", categoria);
            MDC.put("total",     String.valueOf(filtrado.size()));
            LOG.info("productos_filtrados");
            MDC.remove("categoria");
            MDC.remove("total");
        }

        int total = filtrado.size();
        int start = (page - 1) * limit;
        int end   = Math.min(start + limit, total);
        int pages = (int) Math.ceil((double) total / limit);

        List<Producto> pagina = (start < total) ? filtrado.subList(start, end) : List.of();

        var result = new LinkedHashMap<String, Object>();
        result.put("version",   VERSION);
        result.put("total",     total);
        result.put("page",      page);
        result.put("limit",     limit);
        result.put("pages",     pages);
        result.put("productos", pagina);
        result.put("_meta", Map.of("has_next", end < total, "has_prev", page > 1));
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
            .orElseGet(() -> {
                MDC.put("productoId", String.valueOf(id));
                LOG.warn("producto_not_found");   // → stderr en OCP (nivel WARNING)
                MDC.remove("productoId");
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Producto no encontrado")).build();
            });
    }

    // ── GET /version ─────────────────────────────────────────────────────────
    @GET
    @Path("/version")
    public Map<String, Object> version() {
        return Map.of(
            "version",         VERSION,
            "color",           COLOR,
            "host",            System.getenv().getOrDefault("HOSTNAME", "local"),
            "nuevas_features", List.of("paginacion", "filtro_categoria",
                                       "json_logging", "correlation_id")
        );
    }

    // ── GET /stress ──────────────────────────────────────────────────────────
    @GET
    @Path("/stress")
    public Map<String, Object> stress(@QueryParam("seconds") @DefaultValue("5") int seconds) {
        int duration = Math.min(seconds, 30);

        MDC.put("stressDuration", String.valueOf(duration));
        LOG.info("stress_start");
        MDC.remove("stressDuration");

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

    // ── GET /error-demo ──────────────────────────────────────────────────────
    /** Endpoint para demostrar logging de ERROR en el workshop. */
    @GET
    @Path("/error-demo")
    public Response errorDemo() {
        // ERROR va a stderr → OCP lo resalta en la consola
        LOG.error("error_demo_triggered — esto va a stderr");
        throw new RuntimeException("Error de demostración para el workshop");
    }
}
