package com.northstar.commerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommerceEvent(
        UUID eventId,
        EventType type,
        UUID orderId,
        String customerEmail,
        List<LineItem> items,
        BigDecimal total,
        String reason,
        Instant occurredAt
) {
    public CommerceEvent {
        items = items == null ? List.of() : List.copyOf(items);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static CommerceEvent of(EventType type, UUID orderId, String customerEmail,
                                   List<LineItem> items, BigDecimal total, String reason) {
        return new CommerceEvent(UUID.randomUUID(), type, orderId, customerEmail,
                items, total, reason, Instant.now());
    }
}

