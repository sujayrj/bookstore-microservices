package dev.jeppu.domain.product;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
class ProductEntity {
    @Id
    @SequenceGenerator(name = "product_id_seq_gen", sequenceName = "product_id_seq", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_id_seq_gen")
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private BigDecimal price;

    Long getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    String getImageUrl() {
        return imageUrl;
    }

    BigDecimal getPrice() {
        return price;
    }

    void setCode(String code) {
        this.code = code;
    }

    void setName(String name) {
        this.name = name;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    void setPrice(BigDecimal price) {
        this.price = price;
    }
}
