package com.northstar.commerce.inventory.api;

import com.northstar.commerce.inventory.domain.Product;
import com.northstar.commerce.inventory.domain.Reservation;
import com.northstar.commerce.inventory.domain.ReservationStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos {
    private InventoryDtos() {}
    public record ProductRequest(@NotBlank String sku, @NotBlank String name, @Positive BigDecimal price,
                                 @PositiveOrZero int available, @NotBlank String category, String accent) {}
    public record StockAdjustment(@NotNull Integer delta) {}
    public record ProductResponse(String sku, String name, BigDecimal price, int available,
                                  String category, String accent, long version, Instant updatedAt) {
        public static ProductResponse from(Product p) { return new ProductResponse(p.getSku(), p.getName(), p.getPrice(),
                p.getAvailable(), p.getCategory(), p.getAccent(), p.getVersion(), p.getUpdatedAt()); }
    }
    public record ReservationResponse(UUID id, UUID orderId, ReservationStatus status, String reason, Instant createdAt) {
        public static ReservationResponse from(Reservation r) { return new ReservationResponse(r.getId(), r.getOrderId(),
                r.getStatus(), r.getReason(), r.getCreatedAt()); }
    }
}

