package dev.jeppu.bookstore.catalog.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public PagedResponse<Product> getAllProducts(int page) {
        page = page <= 1 ? 0 : page - 1;
        Sort sort = Sort.by(Sort.Direction.ASC, "code");
        PageRequest pageRequest = PageRequest.of(page, 10, sort);
        Page<Product> productPage = productRepository.findAll(pageRequest).map(productMapper::toProduct);
        return new PagedResponse<>(
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber() + 1,
                productPage.isFirst(),
                productPage.isLast(),
                productPage.hasNext(),
                productPage.hasPrevious(),
                productPage.getContent());
    }

    public Optional<Product> findProductByCode(String code) {
        return productRepository.findByCode(code).map(productMapper::toProduct);
    }
}
