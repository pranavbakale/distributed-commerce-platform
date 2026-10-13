package com.northstar.commerce.order.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record OrderItem(String sku, String name, int quantity, BigDecimal unitPrice) {}

