package com.northstar.commerce.payment.messaging;
import com.northstar.commerce.events.*;
import com.northstar.commerce.payment.service.PaymentApplicationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class PaymentEventListener {
    private final PaymentApplicationService service;
    public PaymentEventListener(PaymentApplicationService service){this.service=service;}
    @KafkaListener(topics=Topics.INVENTORY_RESERVED,groupId="payment-service")
    public void charge(CommerceEvent event){service.charge(event);}
}

