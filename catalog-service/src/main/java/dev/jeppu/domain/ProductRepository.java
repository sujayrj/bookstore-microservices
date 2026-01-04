package dev.jeppu.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    @Query("select p from ProductEntity p where p.code=:productCode")
    Optional<ProductEntity> findProductEntityByCode(String productCode);
}
