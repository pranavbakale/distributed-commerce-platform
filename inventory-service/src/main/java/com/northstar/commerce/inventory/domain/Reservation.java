package com.northstar.commerce.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class Reservation {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status;
    private String reason;
    @Column(nullable = false) private Instant createdAt;

    protected Reservation() {}
    public Reservation(UUID orderId, ReservationStatus status, String reason) {
        this.id = UUID.randomUUID(); this.orderId = orderId; this.status = status;
        this.reason = reason; this.createdAt = Instant.now();
    }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public ReservationStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
