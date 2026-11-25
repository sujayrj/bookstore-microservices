package dev.jeppu.web.controller;

import dev.jeppu.domain.product.PageResponse;
import dev.jeppu.domain.product.Product;
import dev.jeppu.domain.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public PageResponse<Product> getAllProducts(@RequestParam(value = "page", defaultValue = "0") Integer pageNo) {
        return productService.getAllProducts(pageNo);
    }

    @GetMapping("/{code}")
    public Product getProductByCode(@PathVariable("code") String code) {
        return productService.getProductByCode(code);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product savedProduct = productService.createProduct(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }
}
