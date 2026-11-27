package dev.jeppu.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_id_seq_gen")
    @SequenceGenerator(name = "order_item_id_seq_gen", sequenceName = "order_item_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Version
    private Long version;

    public void setOrder(OrderEntity orderEntity) {
        this.order = orderEntity;
    }
}
