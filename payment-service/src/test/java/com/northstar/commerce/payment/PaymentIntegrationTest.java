package com.northstar.commerce.payment;

import com.northstar.commerce.events.*;
import com.northstar.commerce.payment.domain.*;
import com.northstar.commerce.payment.service.PaymentApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class PaymentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PaymentRepository payments;
    @Autowired ProcessedEventRepository events;
    @Autowired PaymentApplicationService service;
    @MockBean KafkaTemplate<String, CommerceEvent> kafka;
    @BeforeEach void clean(){events.deleteAll();payments.deleteAll();}

    @Test void capturesAndDeduplicatesPaymentEvent() {
        CommerceEvent event=CommerceEvent.of(EventType.INVENTORY_RESERVED,UUID.randomUUID(),"buyer@example.com",
                List.of(new LineItem("A","Item",1,BigDecimal.TEN)),BigDecimal.TEN,null);
        assertThat(service.charge(event)).isTrue();assertThat(service.charge(event)).isFalse();
        assertThat(payments.count()).isEqualTo(1);
    }

    @Test void declinesConfiguredTestCustomer() {
        CommerceEvent event=CommerceEvent.of(EventType.INVENTORY_RESERVED,UUID.randomUUID(),"decline@example.com",
                List.of(new LineItem("A","Item",1,BigDecimal.TEN)),BigDecimal.TEN,null);
        service.charge(event);
        assertThat(payments.findAll().getFirst().getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @Test void refundsCapturedPayment() throws Exception {
        Payment payment=payments.save(new Payment(UUID.randomUUID(),"buyer@example.com",BigDecimal.TEN,PaymentStatus.CAPTURED,null));
        mvc.perform(post("/api/payments/"+payment.getId()+"/refund")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test void reportsRevenueFromCapturedPaymentsOnly() throws Exception {
        payments.save(new Payment(UUID.randomUUID(),"one@example.com",new BigDecimal("20"),PaymentStatus.CAPTURED,null));
        payments.save(new Payment(UUID.randomUUID(),"two@example.com",new BigDecimal("30"),PaymentStatus.DECLINED,"Declined"));
        mvc.perform(get("/api/payments/metrics/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.revenue").value(20));
    }
}

