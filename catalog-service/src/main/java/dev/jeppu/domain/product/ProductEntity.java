package dev.jeppu.domain.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    @SequenceGenerator(name = "product_id_seq_gen", sequenceName = "product_id_seq", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_id_seq_gen")
    private Long id;

    @NotEmpty(message = "Product Code cannot be blank") @Column(nullable = false, unique = true)
    private String code;

    @NotEmpty(message = "Product Name cannot be blank") @Column(nullable = false)
    private String name;

    private String description;

    private String imageUrl;

    @Column(nullable = false)
    @NotNull(message = "Product Price cannot be null") @DecimalMin("0.1") private BigDecimal price;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
