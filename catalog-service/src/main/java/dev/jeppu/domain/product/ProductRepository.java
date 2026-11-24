package dev.jeppu.domain.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    @Query("select p from ProductEntity p where p.code = :code")
    Optional<ProductEntity> findProductEntityByCode(String code);
}
