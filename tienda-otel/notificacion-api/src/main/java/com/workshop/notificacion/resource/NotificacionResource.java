package com.workshop.notificacion.resource;

import com.workshop.notificacion.model.Notificacion;
import com.workshop.notificacion.service.NotificacionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacionResource {

    @Inject
    NotificacionService service;

    @GET
    public List<Notificacion> listar() {
        return service.listar();
    }

    @POST
    public Response registrar(Map<String, Object> payload) {
        Notificacion n = service.registrar(payload);
        return Response.status(Response.Status.CREATED)
                       .entity(n)
                       .build();
    }
}
