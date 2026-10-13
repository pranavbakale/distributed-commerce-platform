package com.northstar.commerce.inventory.api;

import com.northstar.commerce.inventory.service.InventoryApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InventoryController {
    private final InventoryApplicationService service;
    public InventoryController(InventoryApplicationService service) { this.service = service; }

    @GetMapping("/products")
    public List<InventoryDtos.ProductResponse> products() { return service.products(); }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDtos.ProductResponse add(@Valid @RequestBody InventoryDtos.ProductRequest request) { return service.add(request); }

    @GetMapping("/products/{sku}")
    public InventoryDtos.ProductResponse product(@PathVariable String sku) { return service.product(sku); }

    @PatchMapping("/products/{sku}/stock")
    public InventoryDtos.ProductResponse adjust(@PathVariable String sku,
                                                @Valid @RequestBody InventoryDtos.StockAdjustment request) {
        return service.adjust(sku, request.delta());
    }

    @GetMapping("/inventory/reservations")
    public List<InventoryDtos.ReservationResponse> reservations() { return service.reservations(); }
}

