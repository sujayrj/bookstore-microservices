package dev.jeppu.domain.product;

import dev.jeppu.domain.exception.ProductAlreadyExistsException;
import dev.jeppu.domain.exception.ProductNotFoundException;
import java.util.List;
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
    private final ProductCursorCodec cursorCodec;

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

    /** Cursor-based page size; the DB fetch requests size + 1 rows to detect the next page. */
    private static final int CURSOR_PAGE_SIZE = 10;

    /**
     * Cursor-based listing: anchors to the last returned item's {@code code} instead of a
     * numeric offset, so inserts/deletes between requests cannot shift or duplicate rows.
     *
     * @param cursor opaque token from the previous response, or null for the first page
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<Product> getAllProductsByCursor(String cursor) {
        String lastCode = cursorCodec.decode(cursor);

        List<ProductEntity> rows = productRepository.findByCodeGreaterThan(
                lastCode == null ? "" : lastCode, PageRequest.of(0, CURSOR_PAGE_SIZE + 1, Sort.by("code")));

        boolean hasNext = rows.size() > CURSOR_PAGE_SIZE;
        List<Product> data = rows.stream()
                .limit(CURSOR_PAGE_SIZE)
                .map(ProductMapper::toProduct)
                .toList();
        String nextCursor =
                hasNext ? cursorCodec.encode(rows.get(CURSOR_PAGE_SIZE - 1).getCode()) : null;

        return new CursorPageResponse<>(data, nextCursor, data.size());
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
            throw ProductAlreadyExistsException.forCode(product.code());
        }
        ProductEntity entity = ProductMapper.toProductEntity(product);
        ProductEntity savedProduct = productRepository.save(entity);
        log.info(
                "Product with code : {} was persisted successfully with id : {}",
                savedProduct.getCode(),
                savedProduct.getId());

        return ProductMapper.toProduct(savedProduct);
    }
}
