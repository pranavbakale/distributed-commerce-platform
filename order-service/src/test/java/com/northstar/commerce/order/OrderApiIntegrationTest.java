package com.northstar.commerce.order;

import com.northstar.commerce.events.CommerceEvent;
import com.northstar.commerce.order.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class OrderApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderRepository orders;
    @Autowired ProcessedEventRepository events;
    @MockBean KafkaTemplate<String, CommerceEvent> kafka;

    @BeforeEach void clean(){events.deleteAll();orders.deleteAll();}

    @Test void createsAnOrderAndCalculatesItsTotal() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerEmail":"shopper@example.com","items":[{"sku":"AER-01","name":"Aero Desk Light","quantity":2,"unitPrice":89.00}]}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(178.0))
                .andExpect(jsonPath("$.status").value("PENDING_INVENTORY"));
    }

    @Test void rejectsAnInvalidCheckout() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerEmail":"not-an-email","items":[]}
                """)).andExpect(status().isBadRequest());
    }

    @Test void returnsNotFoundForUnknownOrder() throws Exception {
        mvc.perform(get("/api/orders/"+UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test void summarizesOrderStates() throws Exception {
        OrderEntity pending=new OrderEntity(UUID.randomUUID(),"one@example.com", List.of(new OrderItem("A","A",1, BigDecimal.TEN)),BigDecimal.TEN);
        OrderEntity complete=new OrderEntity(UUID.randomUUID(),"two@example.com",List.of(new OrderItem("B","B",1,BigDecimal.ONE)),BigDecimal.ONE);
        complete.transitionTo(OrderStatus.COMPLETED,null);orders.saveAll(List.of(pending,complete));
        mvc.perform(get("/api/orders/metrics/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.completed").value(1));
    }
}

