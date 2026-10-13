package com.northstar.commerce.inventory.messaging;
import com.northstar.commerce.events.*;
import com.northstar.commerce.inventory.service.InventoryApplicationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class InventoryEventListener {
    private final InventoryApplicationService service;
    public InventoryEventListener(InventoryApplicationService service) { this.service = service; }
    @KafkaListener(topics = Topics.ORDERS_CREATED, groupId = "inventory-service")
    public void reserve(CommerceEvent event) { service.reserve(event); }
    @KafkaListener(topics = Topics.ORDERS_COMPLETED, groupId = "inventory-finalizer")
    public void finalizeReservation(CommerceEvent event) { service.finalizeReservation(event); }
}

