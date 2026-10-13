package com.northstar.commerce.events;

import java.math.BigDecimal;

public record LineItem(String sku, String name, int quantity, BigDecimal unitPrice) {
    public LineItem {
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU is required");
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Price cannot be negative");
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

