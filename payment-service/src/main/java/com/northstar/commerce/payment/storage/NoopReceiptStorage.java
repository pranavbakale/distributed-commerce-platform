package com.northstar.commerce.payment.storage;
import com.northstar.commerce.payment.domain.Payment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="commerce.receipts.enabled", havingValue="false", matchIfMissing=true)
public class NoopReceiptStorage implements ReceiptStorage { public void store(Payment payment) {} }

