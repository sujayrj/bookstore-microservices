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
    void shouldReturnProductForGivenCode() {
        String productCode = "P104";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products/" + productCode)
                .then()
                .body("code", Matchers.is("P104"))
                .body("name", Matchers.is("The Fault in Our Stars"))
                .body("price", Matchers.is(14.5F))
                .body("description", Matchers.notNullValue())
                .body("imageUrl", Matchers.is("https://images.gr-assets.com/books/1360206420l/11870085.jpg"));
    }

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

    @Test
    void shouldReturnProductNotFoundExceptionForInvalidProductCode() {
        String invalidProductCode = "invalid_product_code";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products/" + invalidProductCode)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("title", Matchers.is("Product Not found"))
                .body("timestamp", Matchers.notNullValue())
                .body("errorMessage", Matchers.is("Product with code : invalid_product_code not found"));
    }
}
