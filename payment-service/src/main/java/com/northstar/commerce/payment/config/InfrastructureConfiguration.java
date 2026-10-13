package com.northstar.commerce.payment.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;
@Configuration
public class InfrastructureConfiguration {
    @Bean @ConditionalOnProperty(name="commerce.receipts.enabled",havingValue="true")
    S3Client s3Client(@Value("${aws.region:us-east-1}") String region,
                      @Value("${aws.endpoint:}") String endpoint){
        var builder=S3Client.builder().region(Region.of(region)).credentialsProvider(DefaultCredentialsProvider.create())
                .forcePathStyle(true);
        if(!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }
}

