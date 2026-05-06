package com.example.backend;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/v1/empleados")
@Produces(MediaType.APPLICATION_JSON)
public class EmpleadoResource {

    @GET
    public List<Empleado> getEmpleados() {
        return List.of(
            new Empleado("Juan Pérez", "Desarrollador", 50000.0),
            new Empleado("María García", "Diseñadora", 45000.0),
            new Empleado("Carlos López", "Gerente", 70000.0)
        );
    }
}
