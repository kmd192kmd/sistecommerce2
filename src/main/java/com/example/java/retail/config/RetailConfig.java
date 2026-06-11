package com.example.java.retail.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.retail.v2.PredictionServiceClient;
import com.google.cloud.retail.v2.PredictionServiceSettings;
import com.google.cloud.retail.v2.ProductServiceClient;
import com.google.cloud.retail.v2.ProductServiceSettings;
import com.google.cloud.retail.v2.UserEventServiceClient;
import com.google.cloud.retail.v2.UserEventServiceSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableAsync
public class RetailConfig {

    @Value("${google.cloud.credentials.location}")
    private String credentialsLocation;

    @Value("${retail.location:global}")
    private String location;

    private final ResourceLoader resourceLoader;

    public RetailConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private GoogleCredentials getCredentials() throws IOException {
        Resource resource = resourceLoader.getResource(credentialsLocation);
        try (InputStream inputStream = resource.getInputStream()) {
            return GoogleCredentials.fromStream(inputStream)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        }
    }

    @Bean
    public PredictionServiceClient predictionServiceClient() throws IOException {
        PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(getCredentials()))
                .setEndpoint("retail.googleapis.com:443")
                .build();
        return PredictionServiceClient.create(settings);
    }

    @Bean
    public ProductServiceClient productServiceClient() throws IOException {
        ProductServiceSettings settings = ProductServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(getCredentials()))
                .setEndpoint("retail.googleapis.com:443")
                .build();
        return ProductServiceClient.create(settings);
    }

    @Bean
    public UserEventServiceClient userEventServiceClient() throws IOException {
        UserEventServiceSettings settings = UserEventServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(getCredentials()))
                .setEndpoint("retail.googleapis.com:443")
                .build();
        return UserEventServiceClient.create(settings);
    }
}
