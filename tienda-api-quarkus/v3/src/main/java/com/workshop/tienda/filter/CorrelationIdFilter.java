package com.workshop.tienda.filter;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * CorrelationIdFilter
 * ====================
 * Filtro JAX-RS que propaga el Correlation ID en cada request.
 *
 * Flujo:
 *   1. Request entra  → lee X-Correlation-ID del header (o genera uno nuevo)
 *   2. Lo pone en MDC → aparece automáticamente en TODOS los logs del request
 *   3. Response sale  → devuelve el mismo ID al cliente en el header
 *
 * El MDC (Mapped Diagnostic Context) es un mapa thread-local de JBoss Logging.
 * quarkus-logging-json lo incluye automáticamente en cada línea JSON.
 */
@Provider
@PreMatching   // ejecuta antes del routing, incluso en 404s
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY     = "correlationId";

    // ── Request entrante ─────────────────────────────────────────────────────
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String correlationId = requestContext.getHeaderString(HEADER_NAME);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Inyectar en MDC → el logging-json lo incluye en cada línea automáticamente
        MDC.put(MDC_KEY, correlationId);

        // Pasar el ID al contexto para que el ResponseFilter lo recupere
        requestContext.setProperty(MDC_KEY, correlationId);
    }

    // ── Response saliente ────────────────────────────────────────────────────
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        String correlationId = (String) requestContext.getProperty(MDC_KEY);
        if (correlationId != null) {
            responseContext.getHeaders().add(HEADER_NAME, correlationId);
        }
        // Limpiar el MDC al final del request para evitar fugas entre threads
        MDC.remove(MDC_KEY);
    }
}
