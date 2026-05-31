package com.workshop.pedido.resource;

import com.workshop.pedido.model.Pedido;
import com.workshop.pedido.model.PedidoDto;
import com.workshop.pedido.service.PedidoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    @Inject
    PedidoService service;

    @GET
    public List<Pedido> listar() {
        return service.listar();
    }

    @GET
    @Path("/{id}")
    public Pedido buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @POST
    public Response crear(@Valid PedidoDto.Crear dto) {
        Pedido creado = service.crear(dto);
        return Response.created(URI.create("/pedidos/" + creado.id))
                       .entity(creado)
                       .build();
    }

    @PUT
    @Path("/{id}/estado")
    public Pedido actualizarEstado(
            @PathParam("id") Long id,
            @Valid PedidoDto.ActualizarEstado dto) {
        return service.actualizarEstado(id, dto);
    }
}
