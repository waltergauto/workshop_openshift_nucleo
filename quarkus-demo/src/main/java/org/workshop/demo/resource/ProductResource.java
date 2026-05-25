package org.workshop.demo.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.workshop.demo.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public ProductResource() {
        // Datos iniciales
        save(new Product(null, "Quarkus in Action", "Books",   49.99));
        save(new Product(null, "Raspberry Pi 5",   "Hardware", 89.00));
        save(new Product(null, "OpenShift Guide",  "Books",   39.99));
    }

    @GET
    public List<Product> list() {
        return new ArrayList<>(store.values());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Product p = store.get(id);
        return p != null
            ? Response.ok(p).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    public Response create(Product product) {
        Product created = save(product);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        return store.remove(id) != null
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    private Product save(Product p) {
        p.id = sequence.getAndIncrement();
        store.put(p.id, p);
        return p;
    }
}
