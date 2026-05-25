package org.workshop.demo.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class GreetingResource {

    @GET
    public Map<String, String> info() {
        return Map.of(
            "app",       "quarkus-demo",
            "version",   "1.0.0",
            "timestamp", Instant.now().toString()
        );
    }
}
