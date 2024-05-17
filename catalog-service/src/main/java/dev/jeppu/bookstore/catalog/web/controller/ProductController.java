package dev.jeppu.bookstore.catalog.web.controller;

import dev.jeppu.bookstore.catalog.domain.PagedResponse;
import dev.jeppu.bookstore.catalog.domain.Product;
import dev.jeppu.bookstore.catalog.domain.ProductService;
import dev.jeppu.bookstore.catalog.web.exception.ProductNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PagedResponse<Product> getAllProducts(@RequestParam(name = "page", defaultValue = "1") int page) {
        return productService.getAllProducts(page);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Product> getProductByCode(@PathVariable("code") String code) {
        return productService
                .findProductByCode(code)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ProductNotFoundException.forCode(code));
    }
}
