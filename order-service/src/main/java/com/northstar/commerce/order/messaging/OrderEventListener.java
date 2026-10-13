package com.northstar.commerce.order.messaging;

import com.northstar.commerce.events.CommerceEvent;
import com.northstar.commerce.events.Topics;
import com.northstar.commerce.order.service.OrderApplicationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private final OrderApplicationService service;
    public OrderEventListener(OrderApplicationService service) { this.service = service; }

    @KafkaListener(topics = {Topics.INVENTORY_RESERVED, Topics.PAYMENTS_COMPLETED}, groupId = "order-service")
    public void onWorkflowEvent(CommerceEvent event) { service.apply(event); }
}

