package com.workshop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demuestra los tres tipos principales de métricas custom con Micrometer:
 *
 *   Counter  → cuenta eventos que solo suben (peticiones, errores)
 *   Gauge    → valor que sube y baja (usuarios activos, tamaño de cola)
 *   Timer    → duración de operaciones (latencia, tiempo de proceso)
 *
 * Las métricas automáticas de Quarkus (JVM, HTTP, sistema) se generan
 * sin ningún código adicional — solo con las propiedades en application.properties.
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {

    private static final Logger LOG = Logger.getLogger(MetricsResource.class);

    @Inject
    MeterRegistry registry;

    // ── Estado interno para el Gauge ──────────────────────────────────────
    // AtomicInteger es thread-safe — el Gauge lo lee desde múltiples hilos
    private final AtomicInteger usuariosActivos = new AtomicInteger(0);

    // =========================================================================
    // COUNTER — cuenta eventos que solo se incrementan
    // =========================================================================
    // Caso de uso: total de pedidos procesados, total de errores, logins

    /**
     * GET /api/contador
     * Cada llamada incrementa un contador. Prometheus acumula el total.
     * Métrica generada: workshop_pedidos_total{resultado="exito"|"error"}
     */
    @GET
    @Path("/contador")
    public String demostrarCounter(@QueryParam("error") boolean simularError) {

        Counter counter = Counter.builder("workshop.pedidos")
                .description("Total de pedidos procesados")
                .tag("servicio", "metrics-demo")
                .tag("resultado", simularError ? "error" : "exito")
                .register(registry);

        counter.increment();

        if (simularError) {
            LOG.warn("Pedido procesado con error simulado");
            return "{\"resultado\":\"error\",\"total\":" + (int) counter.count() + "}";
        }

        LOG.infof("Pedido procesado OK. Total acumulado: %.0f", counter.count());
        return "{\"resultado\":\"exito\",\"total\":" + (int) counter.count() + "}";
    }

    // =========================================================================
    // GAUGE — valor que puede subir y bajar
    // =========================================================================
    // Caso de uso: usuarios conectados, tamaño de cola, conexiones abiertas

    /**
     * GET /api/gauge?accion=entrar|salir
     * Simula usuarios que entran y salen. El gauge refleja el valor actual.
     * Métrica generada: workshop_usuarios_activos
     */
    @GET
    @Path("/gauge")
    public String demostrarGauge(@QueryParam("accion") String accion) {

        // El Gauge se registra una sola vez — lee el valor del AtomicInteger
        // en cada scrape de Prometheus (no al llamar a este endpoint)
        Gauge.builder("workshop.usuarios.activos", usuariosActivos, AtomicInteger::get)
                .description("Usuarios activos en el sistema en este momento")
                .tag("servicio", "metrics-demo")
                .register(registry);

        if ("salir".equalsIgnoreCase(accion)) {
            int actual = usuariosActivos.decrementAndGet();
            if (actual < 0) usuariosActivos.set(0);
            LOG.infof("Usuario salió. Activos ahora: %d", usuariosActivos.get());
        } else {
            int actual = usuariosActivos.incrementAndGet();
            LOG.infof("Usuario entró. Activos ahora: %d", actual);
        }

        return "{\"usuarios_activos\":" + usuariosActivos.get() + "}";
    }

    // =========================================================================
    // TIMER — mide duración de operaciones
    // =========================================================================
    // Caso de uso: latencia de llamadas a BD, tiempo de procesamiento

    /**
     * GET /api/timer?ms=200
     * Simula una operación que tarda N milisegundos y la mide con un Timer.
     * Métricas generadas:
     *   workshop_operacion_segundos_count  → cuántas veces se ejecutó
     *   workshop_operacion_segundos_sum    → tiempo total acumulado
     *   workshop_operacion_segundos_max    → máximo registrado
     *   workshop_operacion_segundos_bucket → histograma de percentiles
     */
    @GET
    @Path("/timer")
    public String demostrarTimer(@QueryParam("ms") long milisegundos) {

        long duracion = milisegundos > 0 ? Math.min(milisegundos, 3000) : 100;

        Timer timer = Timer.builder("workshop.operacion")
                .description("Duración de operaciones de negocio")
                .tag("servicio", "metrics-demo")
                .tag("tipo", "simulada")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(registry);

        // Usar record() con Supplier en lugar de recordCallable()
        // Supplier no lanza checked exceptions — no requiere try/catch obligatorio
        long inicio = System.nanoTime();

        try {
            LOG.debugf("Iniciando operación simulada de %dms", duracion);
            Thread.sleep(duracion);
            LOG.infof("Operación completada en %dms", duracion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Operación interrumpida");
        } finally {
            timer.record(System.nanoTime() - inicio, java.util.concurrent.TimeUnit.NANOSECONDS);
        }

        return "{\"duracion_ms\":" + duracion + ",\"estado\":\"completado\"}";
    }

    // =========================================================================
    // MÉTRICAS AUTOMÁTICAS — sin código, solo extensión en pom.xml
    // =========================================================================

    /**
     * GET /api/info
     * No genera métricas custom, pero al llamarlo Quarkus registra
     * automáticamente en /q/metrics:
     *
     *   http_server_requests_seconds_count{method="GET",uri="/api/info",status="200"}
     *   http_server_requests_seconds_sum{...}
     *   http_server_requests_seconds_max{...}
     *
     * Sin escribir una sola línea de código de métricas.
     */
    @GET
    @Path("/info")
    public String info() {
        LOG.info("Endpoint /info llamado");
        return "{\"app\":\"metrics-demo\",\"descripcion\":\"Las métricas HTTP se generan automáticamente\"}";
    }

    /**
     * GET /api/error
     * Simula un error HTTP 500. En /q/metrics aparecerá:
     *   http_server_requests_seconds_count{status="500",...}
     * Útil para disparar la alerta de PrometheusRule.
     */
    @GET
    @Path("/error")
    public jakarta.ws.rs.core.Response simularError() {
        LOG.error("Error simulado para demostración de alertas Prometheus");

        Counter.builder("workshop.errores.http")
                .description("Total de errores HTTP simulados")
                .tag("servicio", "metrics-demo")
                .tag("codigo", "500")
                .register(registry)
                .increment();

        return jakarta.ws.rs.core.Response
                .serverError()
                .entity("{\"error\":\"Error simulado para testing de alertas\"}")
                .build();
    }
}
