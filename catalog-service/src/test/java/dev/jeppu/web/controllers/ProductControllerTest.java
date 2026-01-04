package dev.jeppu.web.controllers;

import dev.jeppu.AbstractIT;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class ProductControllerTest extends AbstractIT {
    @Test
    void shouldReturnProducts() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data", Matchers.hasSize(10))
                .body("totalElements", Matchers.is(15))
                .body("totalPages", Matchers.is(2))
                .body("pageNumber", Matchers.is(1))
                .body("isFirst", Matchers.is(Boolean.TRUE))
                .body("isLast", Matchers.is(Boolean.FALSE))
                .body("hasPrevious", Matchers.is(Boolean.FALSE))
                .body("hasNext", Matchers.is(Boolean.TRUE));
    }
}
