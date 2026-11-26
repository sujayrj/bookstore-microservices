package dev.jeppu.domain.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    @Query("select p from ProductEntity p where p.code = :code")
    Optional<ProductEntity> findProductEntityByCode(String code);

    /**
     * Keyset ("seek") fetch for cursor pagination: returns rows strictly after {@code code}
     * in {@code code} order, limited by the given pageable (the caller requests one extra
     * row to detect the next page without a count query).
     */
    @Query("select p from ProductEntity p where p.code > :code order by p.code asc")
    List<ProductEntity> findByCodeGreaterThan(@Param("code") String code, Pageable pageable);
}
