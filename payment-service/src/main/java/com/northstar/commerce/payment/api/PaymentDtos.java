package com.northstar.commerce.payment.api;
import com.northstar.commerce.payment.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public final class PaymentDtos {
    private PaymentDtos() {}
    public record PaymentResponse(UUID id, UUID orderId, String customerEmail, BigDecimal amount,
                                  PaymentStatus status, String declineReason, Instant createdAt, Instant updatedAt) {
        public static PaymentResponse from(Payment p){return new PaymentResponse(p.getId(),p.getOrderId(),p.getCustomerEmail(),
                p.getAmount(),p.getStatus(),p.getDeclineReason(),p.getCreatedAt(),p.getUpdatedAt());}
    }
    public record PaymentSummary(long total, long captured, long declined, long refunded, BigDecimal revenue) {}
}

