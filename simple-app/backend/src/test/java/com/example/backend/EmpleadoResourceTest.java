package com.example.backend;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class EmpleadoResourceTest {

    @Test
    public void testGetEmpleadosEndpoint() {
        given()
            .when().get("/api/empleados")
            .then()
                .statusCode(200);
    }
}
