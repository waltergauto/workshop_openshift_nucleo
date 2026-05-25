package org.workshop.demo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ProductResourceTest {

    @Test
    void testListProducts() {
        given()
            .when().get("/api/products")
            .then()
            .statusCode(200);
    }

    @Test
    void testInfoEndpoint() {
        given()
            .when().get("/api")
            .then()
            .statusCode(200)
            .body("app", is("quarkus-demo"));
    }
}
