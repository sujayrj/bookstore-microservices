package dev.jeppu.domain.product;

import dev.jeppu.domain.exception.ProductAlreadyExistsException;
import dev.jeppu.domain.exception.ProductNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private static final int PAGE_SIZE = 10;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<Product> getAllProducts(Integer pageNo) {
        pageNo = pageNo <= 1 ? 0 : pageNo - 1;
        Sort sort = Sort.by(Sort.Direction.ASC, "code");
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, sort);
        Page<Product> productEntityPage = productRepository.findAll(pageable).map(ProductMapper::toProduct);

        return new PageResponse<Product>(
                productEntityPage.getTotalElements(),
                productEntityPage.getTotalPages(),
                productEntityPage.getNumber() + 1,
                productEntityPage.isFirst(),
                productEntityPage.isLast(),
                productEntityPage.hasNext(),
                productEntityPage.hasPrevious(),
                productEntityPage.getContent());
    }

    @Transactional(readOnly = true)
    public Product getProductByCode(String code) {
        return productRepository
                .findProductEntityByCode(code)
                .map(ProductMapper::toProduct)
                .orElseThrow(() -> ProductNotFoundException.forCode(code));
    }

    @Transactional
    public Product createProduct(Product product) {
        Optional<ProductEntity> optionalProduct = productRepository.findProductEntityByCode(product.code());
        if (optionalProduct.isPresent()) {
            throw new ProductAlreadyExistsException("Product with code: " + product.code()
                    + " already exists. Cannot create a new Product with same code");
        }
        ProductEntity entity = new ProductEntity();
        entity.setCode(product.code());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setImageUrl(product.imageUrl());
        entity.setPrice(product.price());
        ProductEntity savedProduct = productRepository.save(entity);
        log.info(
                "Product with code : {} was persisted successfully with id : {}",
                savedProduct.getCode(),
                savedProduct.getId());

        return ProductMapper.toProduct(savedProduct);
    }
}
