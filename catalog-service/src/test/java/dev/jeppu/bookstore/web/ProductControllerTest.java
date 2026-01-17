package dev.jeppu.bookstore.web;

import dev.jeppu.AbstractIT;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
public class ProductControllerTest extends AbstractIT {
    @Test
    public void testFindAllProducts() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("totalElements", Matchers.equalTo(14))
                .body("data.size()", Matchers.equalTo(5))
                .body("data[0].code", Matchers.equalTo("P100"));
    }

    @Test
    public void testFindProductByCode() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products/P100")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .body("code", Matchers.equalTo("P100"))
                .body("name", Matchers.equalTo("The Hunger Games"))
                .body("description", Matchers.equalTo("Winning will make you famous. Losing means certain death..."))
                .body("imageUrl", Matchers.equalTo("https://images.gr-assets.com/books/1447303603l/2767052.jpg"))
                .body("price", Matchers.equalTo(34.0F));
    }

    @Test
    public void testFindProductByCodeWhenNotFound() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/products/xxxx")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("title", Matchers.equalTo("Product Not Found"))
                .body("detail", Matchers.equalTo("Product with code : xxxx not found"))
                .body("timestamp", Matchers.notNullValue())
                .body("errorMessage", Matchers.equalTo("Product with code : xxxx not found"));
    }
}
