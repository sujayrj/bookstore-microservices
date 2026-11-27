package dev.jeppu.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_id_seq_gen")
    @SequenceGenerator(name = "order_id_seq_gen", sequenceName = "order_id_seq", allocationSize = 50)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String username;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "customer_name", nullable = false)),
        @AttributeOverride(name = "email", column = @Column(name = "customer_email", nullable = false)),
        @AttributeOverride(name = "phone", column = @Column(name = "customer_phone", nullable = false)),
    })
    private Customer customer;

    @Embedded
    private Address deliveryAddress;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OrderStatus status;

    private String comments;
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItemEntity item) {
        this.items.add(item);
        item.setOrder(this);
    }
}
