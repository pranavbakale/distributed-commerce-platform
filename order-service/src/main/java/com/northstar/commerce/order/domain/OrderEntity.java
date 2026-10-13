package com.northstar.commerce.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String customerEmail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    private String failureReason;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    protected OrderEntity() {}

    public OrderEntity(UUID id, String customerEmail, List<OrderItem> items, BigDecimal total) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.items = new ArrayList<>(items);
        this.total = total;
        this.status = OrderStatus.PENDING_INVENTORY;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void transitionTo(OrderStatus next, String reason) {
        this.status = next;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}

