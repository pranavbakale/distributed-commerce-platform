package com.northstar.commerce.order.domain;

public enum OrderStatus {
    PENDING_INVENTORY,
    PAYMENT_PENDING,
    COMPLETED,
    REJECTED,
    PAYMENT_FAILED,
    CANCELLED
}

