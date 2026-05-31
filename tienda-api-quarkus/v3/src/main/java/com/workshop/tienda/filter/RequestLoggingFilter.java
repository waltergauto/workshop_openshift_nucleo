package com.workshop.tienda.filter;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.io.IOException;

/**
 * RequestLoggingFilter
 * =====================
 * Registra cada request y su respuesta en formato estructurado.
 * El correlationId ya está en MDC (puesto por CorrelationIdFilter),
 * por lo que aparece en cada línea sin agregarlo manualmente.
 *
 * Salida esperada en JSON (quarkus-logging-json):
 * {
 *   "timestamp": "2025-01-15T10:30:00.000Z",
 *   "level":     "INFO",
 *   "loggerName": "com.workshop.tienda.filter.RequestLoggingFilter",
 *   "message":   "request_finished",
 *   "correlationId": "abc-123",    ← viene del MDC automáticamente
 *   "mdc": {
 *     "method":    "GET",
 *     "path":      "/productos",
 *     "status":    200,
 *     "latencyMs": 12
 *   }
 * }
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);

    // Clave para pasar el timestamp de inicio al response filter
    private static final String START_TIME_KEY = "requestStartTime";

    // ── Request entrante ─────────────────────────────────────────────────────
    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        req.setProperty(START_TIME_KEY, System.currentTimeMillis());

        MDC.put("method", req.getMethod());
        MDC.put("path",   req.getUriInfo().getPath());

        LOG.infof("request_started");
    }

    // ── Response saliente ────────────────────────────────────────────────────
    @Override
    public void filter(ContainerRequestContext req,
                       ContainerResponseContext res) throws IOException {

        long startTime = (Long) req.getProperty(START_TIME_KEY);
        long latencyMs = System.currentTimeMillis() - startTime;

        MDC.put("status",    String.valueOf(res.getStatus()));
        MDC.put("latencyMs", String.valueOf(latencyMs));

        if (res.getStatus() >= 500) {
            LOG.errorf("request_finished");
        } else if (res.getStatus() >= 400) {
            LOG.warnf("request_finished");
        } else {
            LOG.infof("request_finished");
        }

        // Limpiar campos de request del MDC
        MDC.remove("method");
        MDC.remove("path");
        MDC.remove("status");
        MDC.remove("latencyMs");
    }
}
