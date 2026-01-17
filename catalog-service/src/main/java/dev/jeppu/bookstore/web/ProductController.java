package dev.jeppu.bookstore.web;

import dev.jeppu.bookstore.domain.PageResponse;
import dev.jeppu.bookstore.domain.Product;
import dev.jeppu.bookstore.domain.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<Product>> getAllProducts(
            @RequestParam(name = "page", required = false, defaultValue = "1") int pageNo) {
        log.debug("getAllProducts method invoked with page number = {}", pageNo);
        PageResponse<Product> allProducts = productService.getAllProducts(pageNo);
        return ResponseEntity.ok(allProducts);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Product> getProductByCode(@PathVariable("code") String productCode) {
        log.debug("getProductCode method invoked for productCode = {}", productCode);
        Product product = productService.getProductByCode(productCode);
        return new ResponseEntity<>(product, HttpStatus.FOUND);
    }
}
