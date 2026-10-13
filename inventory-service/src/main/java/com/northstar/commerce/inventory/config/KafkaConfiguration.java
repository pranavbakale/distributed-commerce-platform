package com.northstar.commerce.inventory.config;
import org.springframework.context.annotation.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
@Configuration
public class KafkaConfiguration {
    @Bean DefaultErrorHandler errorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(500, 2);
        backOff.setMaxInterval(4_000); backOff.setMaxElapsedTime(12_000);
        return new DefaultErrorHandler(backOff);
    }
}

