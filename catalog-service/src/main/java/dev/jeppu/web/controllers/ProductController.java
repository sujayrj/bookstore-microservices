package dev.jeppu.web.controllers;

import dev.jeppu.domain.PagedResult;
import dev.jeppu.domain.ProductResponseDTO;
import dev.jeppu.domain.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
