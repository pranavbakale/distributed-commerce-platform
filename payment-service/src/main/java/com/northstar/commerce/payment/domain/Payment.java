package com.northstar.commerce.payment.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="payments", uniqueConstraints=@UniqueConstraint(columnNames="order_id"))
public class Payment {
    @Id private UUID id;
    @Column(name="order_id",nullable=false) private UUID orderId;
    @Column(nullable=false) private String customerEmail;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PaymentStatus status;
    private String declineReason;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @Version private long version;
    protected Payment() {}
    public Payment(UUID orderId, String customerEmail, BigDecimal amount, PaymentStatus status, String reason) {
        this.id=UUID.randomUUID(); this.orderId=orderId; this.customerEmail=customerEmail; this.amount=amount;
        this.status=status; this.declineReason=reason; this.createdAt=Instant.now(); this.updatedAt=createdAt;
    }
    public void refund() {
        if(status!=PaymentStatus.CAPTURED) throw new IllegalStateException("Only captured payments can be refunded");
        status=PaymentStatus.REFUNDED; updatedAt=Instant.now();
    }
    public UUID getId(){return id;} public UUID getOrderId(){return orderId;} public String getCustomerEmail(){return customerEmail;}
    public BigDecimal getAmount(){return amount;} public PaymentStatus getStatus(){return status;} public String getDeclineReason(){return declineReason;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;}
}
