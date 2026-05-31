package com.workshop.pedido.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

/**
 * Cliente REST para notificacion-api.
 * La URL base se configura en application.properties como
 * quarkus.rest-client.notificacion-api.url
 */
@RegisterRestClient(configKey = "notificacion-api")
@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface NotificacionClient {

    @POST
    Map<String, Object> enviar(Map<String, Object> payload);
}
