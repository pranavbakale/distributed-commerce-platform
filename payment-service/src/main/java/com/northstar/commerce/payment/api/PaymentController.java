package com.northstar.commerce.payment.api;
import com.northstar.commerce.payment.service.PaymentApplicationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController @RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentApplicationService service;
    public PaymentController(PaymentApplicationService service){this.service=service;}
    @GetMapping public List<PaymentDtos.PaymentResponse> all(){return service.all().stream().map(PaymentDtos.PaymentResponse::from).toList();}
    @GetMapping("/{id}") public PaymentDtos.PaymentResponse one(@PathVariable UUID id){return PaymentDtos.PaymentResponse.from(service.find(id));}
    @PostMapping("/{id}/refund") public PaymentDtos.PaymentResponse refund(@PathVariable UUID id){return PaymentDtos.PaymentResponse.from(service.refund(id));}
    @GetMapping("/metrics/summary") public PaymentDtos.PaymentSummary summary(){return service.summary();}
}

