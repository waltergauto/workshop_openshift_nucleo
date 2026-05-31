package com.workshop.pedido.client;

import com.workshop.pedido.model.PedidoDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

/**
 * Cliente REST para producto-api.
 * La URL base se configura en application.properties como
 * quarkus.rest-client.producto-api.url
 *
 * OTel propaga automáticamente el contexto de traza en las
 * cabeceras HTTP salientes (W3C traceparent/tracestate).
 */
@RegisterRestClient(configKey = "producto-api")
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ProductoClient {

    @GET
    @Path("/{id}")
    PedidoDto.ProductoInfo buscarPorId(@PathParam("id") Long id);

    @PUT
    @Path("/{id}/stock")
    PedidoDto.ProductoInfo actualizarStock(
            @PathParam("id") Long id,
            Map<String, Integer> body);
}
