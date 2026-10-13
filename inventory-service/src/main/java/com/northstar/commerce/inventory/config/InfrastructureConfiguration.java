package com.northstar.commerce.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.commerce.inventory.domain.Product;
import com.northstar.commerce.inventory.domain.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import java.math.BigDecimal;
import java.time.Duration;

@Configuration
public class InfrastructureConfiguration {
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer serializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }

    @Bean
    CommandLineRunner seedCatalog(ProductRepository products) {
        return args -> {
            if (products.count() > 0) return;
            products.save(new Product("AER-01", "Aero Desk Light", new BigDecimal("89.00"), 18, "Workspace", "#c9f04a"));
            products.save(new Product("SON-02", "Sonic Headphones", new BigDecimal("179.00"), 11, "Audio", "#ff7a59"));
            products.save(new Product("ARC-03", "Arc Mechanical Keys", new BigDecimal("129.00"), 24, "Workspace", "#8c7dff"));
            products.save(new Product("FLW-04", "Flow Travel Flask", new BigDecimal("42.00"), 35, "Everyday", "#51d6ca"));
            products.save(new Product("NMB-05", "Nimbus Sling", new BigDecimal("74.00"), 14, "Everyday", "#f2c14e"));
            products.save(new Product("FRM-06", "Frame Speaker", new BigDecimal("210.00"), 7, "Audio", "#ef6f9a"));
        };
    }
}
