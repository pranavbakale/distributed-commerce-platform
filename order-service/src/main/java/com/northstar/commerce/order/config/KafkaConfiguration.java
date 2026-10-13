package com.northstar.commerce.order.config;

import com.northstar.commerce.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfiguration {
    @Bean
    NewTopic ordersCreated() { return topic(Topics.ORDERS_CREATED); }
    @Bean
    NewTopic inventoryReserved() { return topic(Topics.INVENTORY_RESERVED); }
    @Bean
    NewTopic paymentsCompleted() { return topic(Topics.PAYMENTS_COMPLETED); }
    @Bean
    NewTopic ordersCompleted() { return topic(Topics.ORDERS_COMPLETED); }

    @Bean
    DefaultErrorHandler errorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(500, 2.0);
        backOff.setMaxInterval(4_000);
        backOff.setMaxElapsedTime(12_000);
        return new DefaultErrorHandler(backOff);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}

