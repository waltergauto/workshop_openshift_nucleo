package com.workshop.producto.resource;

import com.workshop.producto.model.Producto;
import com.workshop.producto.model.ProductoDto;
import com.workshop.producto.service.ProductoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;

@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {

    private static final Logger LOG = Logger.getLogger(ProductoResource.class);

    @Inject
    ProductoService service;

    @GET
    public List<Producto> listar() {
        return service.listar();
    }

    @GET
    @Path("/{id}")
    public Producto buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @POST
    public Response crear(@Valid ProductoDto.Crear dto) {
        Producto creado = service.crear(dto);
        return Response.created(URI.create("/productos/" + creado.id))
                       .entity(creado)
                       .build();
    }

    @PUT
    @Path("/{id}/stock")
    public Producto actualizarStock(
            @PathParam("id") Long id,
            @Valid ProductoDto.ActualizarStock dto) {
        return service.actualizarStock(id, dto);
    }
}
