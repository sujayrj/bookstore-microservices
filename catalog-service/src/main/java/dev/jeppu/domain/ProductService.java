package dev.jeppu.domain;

import dev.jeppu.ApplicationProperties;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ApplicationProperties properties;

    public PagedResult<ProductResponseDTO> getProducts(int pageNo) {
        pageNo = pageNo <= 1 ? 0 : pageNo - 1;
        Sort ascending = Sort.by("name").ascending();
        Pageable pageable = PageRequest.of(pageNo, properties.pageSize(), ascending);
        Page<ProductResponseDTO> productPage =
                productRepository.findAll(pageable).map(ProductMapper::mapToProductResponse);
        PagedResult<ProductResponseDTO> pagedResult = new PagedResult<>(
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber() + 1,
                productPage.isFirst(),
                productPage.isLast(),
                productPage.hasNext(),
                productPage.hasPrevious(),
                productPage.getContent());
        return pagedResult;
    }

    public ProductResponseDTO getProductByCode(String productCode) {
        Optional<ProductEntity> productEntity = productRepository.findProductEntityByCode(productCode);
        ProductEntity foundProduct = productEntity.orElseThrow(() -> ProductNotFoundException.forCode(productCode));
        log.info("Product found with code : {} ", productCode);
        ProductResponseDTO responseDTO = ProductMapper.mapToProductResponse(foundProduct);
        return responseDTO;
    }
}
