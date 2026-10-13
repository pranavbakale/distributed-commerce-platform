package com.northstar.commerce.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private String sku;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private int available;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private String accent;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected Product() {}
    public Product(String sku, String name, BigDecimal price, int available, String category, String accent) {
        this.sku = sku; this.name = name; this.price = price; this.available = available;
        this.category = category; this.accent = accent; this.updatedAt = Instant.now();
    }
    public void reserve(int quantity) {
        if (quantity < 1 || available < quantity) throw new IllegalStateException("Insufficient stock for " + sku);
        available -= quantity; updatedAt = Instant.now();
    }
    public void release(int quantity) { available += quantity; updatedAt = Instant.now(); }
    public void adjust(int delta) {
        if (available + delta < 0) throw new IllegalArgumentException("Stock cannot be negative");
        available += delta; updatedAt = Instant.now();
    }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getAvailable() { return available; }
    public String getCategory() { return category; }
    public String getAccent() { return accent; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

