package com.northstar.commerce.payment.storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.commerce.payment.domain.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.Map;
@Component
@ConditionalOnProperty(name="commerce.receipts.enabled", havingValue="true")
public class S3ReceiptStorage implements ReceiptStorage {
    private final S3Client s3; private final ObjectMapper json; private final String bucket;
    public S3ReceiptStorage(S3Client s3, ObjectMapper json, @Value("${commerce.receipts.bucket}") String bucket) {
        this.s3=s3;this.json=json;this.bucket=bucket; ensureBucket();
    }
    private void ensureBucket(){
        try{s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());}
        catch(S3Exception ex){s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());}
    }
    @Override public void store(Payment p){
        try{
            byte[] body=json.writeValueAsBytes(Map.of("paymentId",p.getId(),"orderId",p.getOrderId(),"amount",p.getAmount(),
                    "status",p.getStatus(),"capturedAt",p.getCreatedAt()));
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key("receipts/"+p.getId()+".json").contentType("application/json").build(),RequestBody.fromBytes(body));
        }catch(Exception ex){throw new IllegalStateException("Could not store receipt",ex);}
    }
}

