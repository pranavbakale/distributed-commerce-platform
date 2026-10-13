package com.northstar.commerce.inventory.service;

import com.northstar.commerce.events.*;
import com.northstar.commerce.inventory.api.InventoryDtos;
import com.northstar.commerce.inventory.domain.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class InventoryApplicationService {
    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final ProcessedEventRepository processedEvents;
    private final KafkaTemplate<String, CommerceEvent> kafka;

    public InventoryApplicationService(ProductRepository products, ReservationRepository reservations,
                                       ProcessedEventRepository processedEvents,
                                       KafkaTemplate<String, CommerceEvent> kafka) {
        this.products = products; this.reservations = reservations;
        this.processedEvents = processedEvents; this.kafka = kafka;
    }

    @Transactional(readOnly = true)
    public List<InventoryDtos.ProductResponse> products() {
        return products.findAll(Sort.by("name")).stream().map(InventoryDtos.ProductResponse::from).toList();
    }

    @Cacheable(cacheNames = "products", key = "#sku")
    @Transactional(readOnly = true)
    public InventoryDtos.ProductResponse product(String sku) {
        return InventoryDtos.ProductResponse.from(products.findById(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product not found")));
    }

    @CacheEvict(cacheNames = "products", key = "#request.sku()")
    @Transactional
    public InventoryDtos.ProductResponse add(InventoryDtos.ProductRequest request) {
        if (products.existsById(request.sku())) throw new IllegalStateException("SKU already exists");
        String accent = request.accent() == null || request.accent().isBlank() ? "#c9f04a" : request.accent();
        return InventoryDtos.ProductResponse.from(products.save(new Product(request.sku(), request.name(),
                request.price(), request.available(), request.category(), accent)));
    }

    @CacheEvict(cacheNames = "products", key = "#sku")
    @Transactional
    public InventoryDtos.ProductResponse adjust(String sku, int delta) {
        Product product = products.findById(sku).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        product.adjust(delta);
        return InventoryDtos.ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<InventoryDtos.ReservationResponse> reservations() {
        return reservations.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(InventoryDtos.ReservationResponse::from).toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public boolean reserve(CommerceEvent event) {
        if (processedEvents.existsById(event.eventId())) return false;
        if (reservations.findByOrderId(event.orderId()).isPresent()) {
            processedEvents.save(new ProcessedEvent(event.eventId()));
            return false;
        }

        Map<String, Product> requested = new LinkedHashMap<>();
        String rejection = null;
        for (LineItem item : event.items()) {
            Product product = products.findById(item.sku()).orElse(null);
            if (product == null) { rejection = "Unknown product " + item.sku(); break; }
            if (product.getAvailable() < item.quantity()) { rejection = "Insufficient stock for " + item.name(); break; }
            requested.put(item.sku(), product);
        }

        if (rejection != null) {
            reservations.save(new Reservation(event.orderId(), ReservationStatus.REJECTED, rejection));
            processedEvents.save(new ProcessedEvent(event.eventId()));
            kafka.send(Topics.INVENTORY_RESERVED, event.orderId().toString(), CommerceEvent.of(
                    EventType.INVENTORY_REJECTED, event.orderId(), event.customerEmail(), event.items(),
                    event.total(), rejection));
            return true;
        }

        for (LineItem item : event.items()) requested.get(item.sku()).reserve(item.quantity());
        reservations.save(new Reservation(event.orderId(), ReservationStatus.RESERVED, null));
        processedEvents.save(new ProcessedEvent(event.eventId()));
        kafka.send(Topics.INVENTORY_RESERVED, event.orderId().toString(), CommerceEvent.of(
                EventType.INVENTORY_RESERVED, event.orderId(), event.customerEmail(), event.items(),
                event.total(), null));
        return true;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public boolean finalizeReservation(CommerceEvent event) {
        if (processedEvents.existsById(event.eventId())) return false;
        Reservation reservation = reservations.findByOrderId(event.orderId()).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.RESERVED) {
            processedEvents.save(new ProcessedEvent(event.eventId()));
            return false;
        }
        if (event.type() == EventType.ORDER_COMPLETED) {
            reservation.setStatus(ReservationStatus.COMMITTED);
        } else if (event.type() == EventType.ORDER_CANCELLED) {
            for (LineItem item : event.items()) {
                products.findById(item.sku()).ifPresent(product -> product.release(item.quantity()));
            }
            reservation.setStatus(ReservationStatus.RELEASED);
        }
        processedEvents.save(new ProcessedEvent(event.eventId()));
        return true;
    }
}

