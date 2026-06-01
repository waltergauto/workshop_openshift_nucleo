package com.workshop;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;

/**
 * Filtro JAX-RS que enriquece el MDC automáticamente en cada petición.
 *
 * @Provider hace que JAX-RS lo registre automáticamente — no hace
 * falta declararlo en ningún otro lugar.
 *
 * Al implementar tanto ContainerRequestFilter como ContainerResponseFilter
 * podemos medir el tiempo de la petición y loguearlo en la respuesta.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    // Clave para guardar el tiempo de inicio en el contexto de la petición
    private static final String START_TIME_KEY = "startTime";

    /**
     * Se ejecuta ANTES del método del Resource.
     * Puebla el MDC con datos de la petición para que aparezcan
     * en TODOS los logs emitidos durante su procesamiento.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Generar o propagar un requestId para trazabilidad
        String requestId = requestContext.getHeaderString("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Guardar tiempo de inicio para calcular latencia en la respuesta
        requestContext.setProperty(START_TIME_KEY, System.currentTimeMillis());

        // Poblar el MDC — estos campos aparecerán en todos los logs siguientes
        MDC.put("requestId", requestId);
        MDC.put("method",    requestContext.getMethod());
        MDC.put("path",      requestContext.getUriInfo().getPath());

        // Log de entrada de la petición
        LOG.infof("→ %s %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri());
    }

    /**
     * Se ejecuta DESPUÉS del método del Resource, al generar la respuesta.
     * Loguea el resultado y limpia el MDC.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        try {
            // Calcular latencia
            Long startTime = (Long) requestContext.getProperty(START_TIME_KEY);
            long latencyMs = startTime != null
                ? System.currentTimeMillis() - startTime
                : -1;

            int status = responseContext.getStatus();

            // Log de salida con latencia y código HTTP
            if (status >= 500) {
                LOG.errorf("← %d %s %s (%dms)",
                    status,
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    latencyMs);
            } else if (status >= 400) {
                LOG.warnf("← %d %s %s (%dms)",
                    status,
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    latencyMs);
            } else {
                LOG.infof("← %d %s %s (%dms)",
                    status,
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    latencyMs);
            }
        } finally {
            // Limpiar SIEMPRE en el finally — evita fugas de contexto
            // entre peticiones en el mismo hilo (thread pool de Quarkus)
            MDC.clear();
        }
    }
}
