package dev.jeppu.bookstore.domain;

import dev.jeppu.bookstore.ApplicationProperties;
import dev.jeppu.bookstore.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationProperties properties;

    public PageResponse<Product> getAllProducts(int pageNo) {
        pageNo = pageNo <= 1 ? 0 : pageNo - 1;
        Pageable pageable = PageRequest.of(pageNo, properties.pageSize());
        Page<Product> productEntityPage = productRepository.findAll(pageable).map(ProductMapper::toDTO);
        PageResponse<Product> pageResponse = new PageResponse<>(
                productEntityPage.getTotalElements(),
                productEntityPage.getTotalPages(),
                productEntityPage.getNumber() + 1,
                productEntityPage.isFirst(),
                productEntityPage.isLast(),
                productEntityPage.hasNext(),
                productEntityPage.hasPrevious(),
                productEntityPage.getContent());
        log.debug("Found {} number of products in DB ", pageResponse.data().size());
        return pageResponse;
    }

    public Product getProductByCode(String productCode) {
        ProductEntity foundProductEntity = productRepository
                .findProductEntityByCode(productCode)
                .orElseThrow(() -> ProductNotFoundException.forCode(productCode));
        Product foundProduct = ProductMapper.toDTO(foundProductEntity);
        log.debug("Found product : {} from DB", foundProduct);
        return foundProduct;
    }
}
