package ca.sheridancollege.koonerga.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion("us-east-2")
            .build();

    private String readObject(String key) {
        try (S3Object object = s3.getObject(bucketName, key);
             InputStream input = object.getObjectContent()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to fetch " + key + " from S3: " + e.getMessage() + "\"}";
        }
    }

    public String getLatestScanResults() {
        return readObject("latest/scan_results.json");
    }

    public String getLatestAlerts() {
        return readObject("latest/alert.json");
    }
}

