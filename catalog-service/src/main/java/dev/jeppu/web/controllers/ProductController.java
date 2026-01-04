package dev.jeppu.web.controllers;

import dev.jeppu.domain.PagedResult;
import dev.jeppu.domain.ProductResponseDTO;
import dev.jeppu.domain.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
class ProductController {
    private final ProductService productService;

    @GetMapping
    PagedResult<ProductResponseDTO> getProducts(@RequestParam(name = "page", defaultValue = "1") int pageNo) {
        PagedResult<ProductResponseDTO> products = productService.getProducts(pageNo);
        return products;
    }

    @GetMapping("/{productCode}")
    ResponseEntity<ProductResponseDTO> getProductByCode(@PathVariable String productCode) {
        ProductResponseDTO productDTO = productService.getProductByCode(productCode);
        return new ResponseEntity<>(productDTO, HttpStatus.FOUND);
    }
}
