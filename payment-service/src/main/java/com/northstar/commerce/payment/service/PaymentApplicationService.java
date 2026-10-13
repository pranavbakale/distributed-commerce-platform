package com.northstar.commerce.payment.service;

import com.northstar.commerce.events.*;
import com.northstar.commerce.payment.api.PaymentDtos;
import com.northstar.commerce.payment.domain.*;
import com.northstar.commerce.payment.storage.ReceiptStorage;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentApplicationService {
    private final PaymentRepository payments;
    private final ProcessedEventRepository processedEvents;
    private final KafkaTemplate<String, CommerceEvent> kafka;
    private final ReceiptStorage receipts;

    public PaymentApplicationService(PaymentRepository payments, ProcessedEventRepository processedEvents,
                                     KafkaTemplate<String, CommerceEvent> kafka, ReceiptStorage receipts) {
        this.payments=payments; this.processedEvents=processedEvents; this.kafka=kafka; this.receipts=receipts;
    }

    @Transactional
    public boolean charge(CommerceEvent event) {
        if (processedEvents.existsById(event.eventId())) return false;
        if (event.type() != EventType.INVENTORY_RESERVED || payments.findByOrderId(event.orderId()).isPresent()) {
            processedEvents.save(new ProcessedEvent(event.eventId()));
            return false;
        }
        boolean declined = event.customerEmail().toLowerCase().contains("decline");
        String reason = declined ? "Payment method was declined" : null;
        Payment payment = payments.save(new Payment(event.orderId(), event.customerEmail(), event.total(),
                declined ? PaymentStatus.DECLINED : PaymentStatus.CAPTURED, reason));
        processedEvents.save(new ProcessedEvent(event.eventId()));
        EventType result = declined ? EventType.PAYMENT_FAILED : EventType.PAYMENT_COMPLETED;
        kafka.send(Topics.PAYMENTS_COMPLETED, event.orderId().toString(), CommerceEvent.of(
                result, event.orderId(), event.customerEmail(), event.items(), event.total(), reason));
        if (!declined) receipts.store(payment);
        return true;
    }

    @Transactional(readOnly=true)
    public List<Payment> all(){return payments.findAll(Sort.by(Sort.Direction.DESC,"createdAt"));}
    @Transactional(readOnly=true)
    public Payment find(UUID id){return payments.findById(id).orElseThrow(()->new EntityNotFoundException("Payment not found"));}
    @Transactional
    public Payment refund(UUID id){Payment payment=find(id);payment.refund();return payment;}

    @Transactional(readOnly=true)
    public PaymentDtos.PaymentSummary summary(){
        List<Payment> all=payments.findAll();
        BigDecimal revenue=all.stream().filter(p->p.getStatus()==PaymentStatus.CAPTURED)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new PaymentDtos.PaymentSummary(all.size(), payments.countByStatus(PaymentStatus.CAPTURED),
                payments.countByStatus(PaymentStatus.DECLINED), payments.countByStatus(PaymentStatus.REFUNDED), revenue);
    }
}

