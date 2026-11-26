package dev.jeppu.web.controller;

import dev.jeppu.domain.product.CursorPageResponse;
import dev.jeppu.domain.product.PageResponse;
import dev.jeppu.domain.product.Product;
import dev.jeppu.domain.product.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public PageResponse<Product> getAllProducts(@RequestParam(value = "page", defaultValue = "1") Integer pageNo) {
        return productService.getAllProducts(pageNo);
    }

    /**
     * Cursor-based variant of the product listing. Walk pages forward by passing the
     * {@code nextCursor} value from each response back as {@code cursor}; a null
     * {@code nextCursor} means you've reached the end.
     */
    @GetMapping("/cursor")
    public CursorPageResponse<Product> getAllProductsByCursor(
            @RequestParam(value = "cursor", required = false) String cursor) {
        return productService.getAllProductsByCursor(cursor);
    }

    @GetMapping("/{code}")
    public Product getProductByCode(@PathVariable("code") String code) {
        return productService.getProductByCode(code);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product savedProduct = productService.createProduct(product);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(savedProduct.code())
                .toUri();
        return ResponseEntity.created(uri).body(savedProduct);
    }
}
