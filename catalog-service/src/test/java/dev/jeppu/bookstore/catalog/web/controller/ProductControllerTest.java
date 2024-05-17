package dev.jeppu.bookstore.catalog.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jeppu.bookstore.catalog.AbstractIntTest;
import dev.jeppu.bookstore.catalog.domain.Product;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class ProductControllerTest extends AbstractIntTest {

    @Test
    void verifyAllProducts() {
        RestAssured.given()
                .when()
                .contentType(ContentType.JSON)
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("data", Matchers.hasSize(10))
                .body("totalElements", Matchers.is(15))
                .body("totalPages", Matchers.is(2))
                .body("pageNumber", Matchers.is(1))
                .body("isFirst", Matchers.is(true))
                .body("isLast", Matchers.is(false))
                .body("hasPrevious", Matchers.is(false))
                .body("hasNext", Matchers.is(true));
    }

    @Test
    void verifyProductByCode() {
        String code = "P100";
        Product actualProduct = RestAssured.given()
                .when()
                .contentType(ContentType.JSON)
                .get("/api/products/{code}", code)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Product.class);
        assertThat(actualProduct.code()).isEqualTo("P100");
        assertThat(actualProduct.name()).isEqualTo("The Hunger Games");
        assertThat(actualProduct.description())
                .isEqualTo("Winning will make you famous. Losing means certain death...");
        assertThat(actualProduct.imageUrl()).isEqualTo("https://images.gr-assets.com/books/1447303603l/2767052.jpg");
        assertThat(actualProduct.price()).isEqualTo("34.0");
    }

    @Test
    void verifyProductNotFoundExceptionWhenProductDoesnotExist() {
        String code = "P100000";
        RestAssured.given()
                .when()
                .contentType(ContentType.JSON)
                .get("/api/products/{code}", code)
                .then()
                .statusCode(404)
                .body("title", Matchers.is("Product Not Found"))
                .body("detail", Matchers.is("Product not found for code: P100000"))
                .body("service_name", Matchers.is("Catalog Service"))
                .body("error_category", Matchers.is("Generic"))
                .body("instance", Matchers.is("/api/products/P100000"));
    }
}
