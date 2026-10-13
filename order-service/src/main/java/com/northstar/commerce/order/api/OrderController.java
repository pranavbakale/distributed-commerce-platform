package com.northstar.commerce.order.api;

import com.northstar.commerce.order.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService service;

    public OrderController(OrderApplicationService service) { this.service = service; }

    @GetMapping
    public List<OrderDtos.OrderResponse> all() {
        return service.findAll().stream().map(OrderDtos.OrderResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.OrderResponse create(@Valid @RequestBody OrderDtos.CreateOrderRequest request) {
        return OrderDtos.OrderResponse.from(service.create(request));
    }

    @GetMapping("/{id}")
    public OrderDtos.OrderResponse one(@PathVariable UUID id) {
        return OrderDtos.OrderResponse.from(service.find(id));
    }

    @PostMapping("/{id}/cancel")
    public OrderDtos.OrderResponse cancel(@PathVariable UUID id) {
        return OrderDtos.OrderResponse.from(service.cancel(id));
    }

    @GetMapping("/metrics/summary")
    public OrderDtos.OrderSummary summary() { return service.summary(); }
}

