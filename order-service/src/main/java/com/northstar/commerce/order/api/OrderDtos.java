package com.northstar.commerce.order.api;

import com.northstar.commerce.order.domain.OrderEntity;
import com.northstar.commerce.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(
            @NotNull @Email String customerEmail,
            @NotEmpty List<@Valid ItemRequest> items
    ) {}

    public record ItemRequest(
            @NotNull String sku,
            @NotNull String name,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitPrice
    ) {}

    public record OrderResponse(UUID id, String customerEmail, OrderStatus status,
                                BigDecimal total, String failureReason, Instant createdAt,
                                Instant updatedAt, List<ItemResponse> items) {
        public static OrderResponse from(OrderEntity order) {
            return new OrderResponse(order.getId(), order.getCustomerEmail(), order.getStatus(),
                    order.getTotal(), order.getFailureReason(), order.getCreatedAt(), order.getUpdatedAt(),
                    order.getItems().stream().map(i -> new ItemResponse(
                            i.sku(), i.name(), i.quantity(), i.unitPrice())).toList());
        }
    }

    public record ItemResponse(String sku, String name, int quantity, BigDecimal unitPrice) {}
    public record OrderSummary(long total, long pending, long completed, long failed) {}
}

