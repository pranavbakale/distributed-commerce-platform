package com.northstar.commerce.inventory;

import com.northstar.commerce.events.*;
import com.northstar.commerce.inventory.domain.*;
import com.northstar.commerce.inventory.service.InventoryApplicationService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class InventoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired ReservationRepository reservations;
    @Autowired ProcessedEventRepository events;
    @Autowired InventoryApplicationService service;
    @MockBean KafkaTemplate<String, CommerceEvent> kafka;

    @BeforeEach void reset(){events.deleteAll();reservations.deleteAll();products.deleteAll();products.save(new Product("TEST-1","Test Product",new BigDecimal("25.00"),5,"Test","#ffffff"));}

    @Test void listsProducts() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].sku").value("TEST-1"));
    }

    @Test void adjustsStockWithVersionedEntity() throws Exception {
        mvc.perform(patch("/api/products/TEST-1/stock").contentType(MediaType.APPLICATION_JSON).content("{\"delta\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(8));
        assertThat(products.findById("TEST-1").orElseThrow().getVersion()).isPositive();
    }

    @Test void reservesInventoryAndIgnoresDuplicateEvent() {
        UUID eventId=UUID.randomUUID();UUID orderId=UUID.randomUUID();
        CommerceEvent event=new CommerceEvent(eventId,EventType.ORDER_CREATED,orderId,"buyer@example.com",
                List.of(new LineItem("TEST-1","Test Product",2,new BigDecimal("25.00"))),new BigDecimal("50.00"),null,null);
        assertThat(service.reserve(event)).isTrue();
        assertThat(service.reserve(event)).isFalse();
        assertThat(products.findById("TEST-1").orElseThrow().getAvailable()).isEqualTo(3);
    }

    @Test void rejectsReservationWhenStockIsShort() {
        CommerceEvent event=CommerceEvent.of(EventType.ORDER_CREATED,UUID.randomUUID(),"buyer@example.com",
                List.of(new LineItem("TEST-1","Test Product",8,new BigDecimal("25.00"))),new BigDecimal("200.00"),null);
        assertThat(service.reserve(event)).isTrue();
        assertThat(reservations.findAll().getFirst().getStatus()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(products.findById("TEST-1").orElseThrow().getAvailable()).isEqualTo(5);
    }
}

