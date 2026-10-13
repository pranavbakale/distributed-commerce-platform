package com.northstar.commerce.order.service;

import com.northstar.commerce.events.CommerceEvent;
import com.northstar.commerce.events.EventType;
import com.northstar.commerce.events.LineItem;
import com.northstar.commerce.events.Topics;
import com.northstar.commerce.order.api.OrderDtos;
import com.northstar.commerce.order.domain.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orders;
    private final ProcessedEventRepository processedEvents;
    private final KafkaTemplate<String, CommerceEvent> kafka;

    public OrderApplicationService(OrderRepository orders, ProcessedEventRepository processedEvents,
                                   KafkaTemplate<String, CommerceEvent> kafka) {
        this.orders = orders;
        this.processedEvents = processedEvents;
        this.kafka = kafka;
    }

    @Transactional
    public OrderEntity create(OrderDtos.CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.sku(), i.name(), i.quantity(), i.unitPrice())).toList();
        BigDecimal total = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        OrderEntity order = orders.save(new OrderEntity(UUID.randomUUID(), request.customerEmail(), items, total));
        kafka.send(Topics.ORDERS_CREATED, order.getId().toString(), CommerceEvent.of(
                EventType.ORDER_CREATED, order.getId(), order.getCustomerEmail(), toLineItems(order), total, null));
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> findAll() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public OrderEntity find(UUID id) {
        return orders.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    @Transactional
    public OrderEntity cancel(UUID id) {
        OrderEntity order = find(id);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Completed or cancelled orders cannot be cancelled");
        }
        order.transitionTo(OrderStatus.CANCELLED, "Cancelled by customer");
        kafka.send(Topics.ORDERS_COMPLETED, id.toString(), CommerceEvent.of(
                EventType.ORDER_CANCELLED, id, order.getCustomerEmail(), toLineItems(order), order.getTotal(), null));
        return order;
    }

    @Transactional
    public boolean apply(CommerceEvent event) {
        if (processedEvents.existsById(event.eventId())) return false;
        OrderEntity order = find(event.orderId());
        switch (event.type()) {
            case INVENTORY_RESERVED -> order.transitionTo(OrderStatus.PAYMENT_PENDING, null);
            case INVENTORY_REJECTED -> order.transitionTo(OrderStatus.REJECTED, event.reason());
            case PAYMENT_COMPLETED -> {
                order.transitionTo(OrderStatus.COMPLETED, null);
                kafka.send(Topics.ORDERS_COMPLETED, order.getId().toString(), CommerceEvent.of(
                        EventType.ORDER_COMPLETED, order.getId(), order.getCustomerEmail(),
                        toLineItems(order), order.getTotal(), null));
            }
            case PAYMENT_FAILED -> {
                order.transitionTo(OrderStatus.PAYMENT_FAILED, event.reason());
                kafka.send(Topics.ORDERS_COMPLETED, order.getId().toString(), CommerceEvent.of(
                        EventType.ORDER_CANCELLED, order.getId(), order.getCustomerEmail(),
                        toLineItems(order), order.getTotal(), event.reason()));
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + event.type());
        }
        processedEvents.save(new ProcessedEvent(event.eventId()));
        return true;
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderSummary summary() {
        long total = orders.count();
        long pending = orders.countByStatus(OrderStatus.PENDING_INVENTORY)
                + orders.countByStatus(OrderStatus.PAYMENT_PENDING);
        long completed = orders.countByStatus(OrderStatus.COMPLETED);
        return new OrderDtos.OrderSummary(total, pending, completed, total - pending - completed);
    }

    private List<LineItem> toLineItems(OrderEntity order) {
        return order.getItems().stream()
                .map(i -> new LineItem(i.sku(), i.name(), i.quantity(), i.unitPrice())).toList();
    }
}
