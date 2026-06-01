package com.workshop;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Recurso REST de demostración de logging en Quarkus.
 *
 * Cómo obtener el Logger:
 *   - Logger.getLogger(Clase.class) → estático, funciona en cualquier contexto
 *   - @Inject Logger                → CDI, solo en beans gestionados
 *
 * Ambos producen el mismo output. En este ejemplo usamos @Inject
 * para mostrar que Quarkus lo soporta nativamente sin configuración extra.
 */
@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
public class ExampleResource {

    // Quarkus inyecta un Logger con el nombre de la clase actual
    @Inject
    Logger log;

    /**
     * GET /hello
     * Demuestra los niveles de log y cuándo usar cada uno.
     */
    @GET
    public String sayHello() {

        // ── TRACE ─────────────────────────────────────────────────────
        // El más verbose. Activo solo con quarkus.log.level=TRACE.
        // Útil para trazar el flujo interno de un método.
        log.trace("TRACE: entrando a sayHello()");

        // ── DEBUG ─────────────────────────────────────────────────────
        // Información de diagnóstico. Activo con quarkus.log.level=DEBUG.
        // En producción suele estar desactivado a nivel global pero
        // activado por paquete: quarkus.log.category."com.workshop".level=DEBUG
        log.debug("DEBUG: preparando respuesta del endpoint /hello");

        // ── INFO ──────────────────────────────────────────────────────
        // El nivel por defecto en producción.
        // Registrar eventos de negocio relevantes: petición recibida,
        // recurso creado, proceso completado.
        log.info("INFO: Generating an INFO log message!");

        // ── WARN ──────────────────────────────────────────────────────
        // Algo inesperado ocurrió pero la app puede continuar.
        // Ejemplos: retry de una llamada HTTP, caché miss, deprecación.
        log.warn("WARN: este endpoint es de demo, no usar en producción");

        // ── ERROR ─────────────────────────────────────────────────────
        // Fallo que requiere atención. Siempre incluir la excepción
        // cuando existe para obtener el stacktrace en el log.
        log.error("ERROR: An example ERROR log message occurred.");

        // Ejemplo de ERROR con excepción real (lo mostramos sin lanzarla)
        try {
            // Simulamos una operación que puede fallar
            String resultado = operacionQueSimulaFallo();
            log.infof("Operación completada con resultado: %s", resultado);
        } catch (Exception e) {
            // BIEN: pasar la excepción como primer argumento
            // imprime el stacktrace completo en el log JSON
            log.errorf(e, "Error en operacionQueSimulaFallo: %s", e.getMessage());
        }

        return "Hello from Quarkus";
    }

    /**
     * GET /hello/contexto?nombre=Juan
     * Demuestra el uso de MDC para enriquecer todos los logs de una petición.
     */
    @GET
    @Path("/contexto")
    public String sayHelloConContexto(@QueryParam("nombre") String nombre) {

        // MDC (Mapped Diagnostic Context): adjunta pares clave-valor
        // al contexto del hilo actual. Aparecen automáticamente en TODOS
        // los logs que se emitan durante esta petición, sin pasarlos manualmente.
        MDC.put("usuario",    nombre != null ? nombre : "anonimo");
        MDC.put("endpoint",   "/hello/contexto");
        MDC.put("requestId",  java.util.UUID.randomUUID().toString());

        try {
            log.info("Petición recibida");         // incluye usuario, endpoint, requestId
            log.debugf("Parámetro nombre='%s'", nombre);
            procesarSaludo(nombre);
            log.info("Respuesta generada");        // también incluye el MDC
            return "Hola " + (nombre != null ? nombre : "Mundo") + " desde Quarkus";
        } finally {
            // IMPORTANTE: limpiar el MDC al finalizar la petición
            // para que no contamine peticiones posteriores del mismo hilo
            MDC.clear();
        }
    }

    /**
     * GET /hello/niveles
     * Dispara un log de cada nivel para ver el efecto en consola/JSON.
     */
    @GET
    @Path("/niveles")
    public String demostrarNiveles() {
        log.trace("Nivel TRACE  — más detallado");
        log.debug("Nivel DEBUG  — diagnóstico");
        log.info ("Nivel INFO   — evento normal");
        log.warn ("Nivel WARN   — advertencia");
        log.error("Nivel ERROR  — fallo a atender");
        return "Logs emitidos — revisá la consola o oc logs";
    }

    // ── Métodos auxiliares de demostración ────────────────────────────────

    private String operacionQueSimulaFallo() {
        // Simulamos que a veces falla para mostrar el log de error con excepción
        if (Math.random() < 0.5) {
            throw new RuntimeException("Fallo simulado para demostración de logging");
        }
        return "OK";
    }

    private void procesarSaludo(String nombre) {
        log.debugf("procesarSaludo llamado con nombre='%s'", nombre);
        // lógica de negocio...
    }
}
