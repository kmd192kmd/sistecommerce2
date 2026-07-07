package com.example.java.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";
    public static final String PAYMENT_FAILED_TOPIC = "payment-failed-topic";
    public static final String PAYMENT_SUCCESS_DLQ = "payment-success-topic.DLQ";
    public static final String PAYMENT_FAILED_DLQ = "payment-failed-topic.DLQ";

    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name(PAYMENT_SUCCESS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(PAYMENT_FAILED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentSuccessDlq() {
        return TopicBuilder.name(PAYMENT_SUCCESS_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedDlq() {
        return TopicBuilder.name(PAYMENT_FAILED_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public org.springframework.kafka.listener.DefaultErrorHandler errorHandler(
            org.springframework.kafka.core.KafkaTemplate<Object, Object> template) {
        
        org.springframework.kafka.listener.DeadLetterPublishingRecoverer recoverer = 
                new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(template);
        
        // 재시도 주기 2초, 최대 재시도 횟수 2회 (최초 시도 포함 총 3회 실행)
        org.springframework.util.backoff.FixedBackOff backOff = new org.springframework.util.backoff.FixedBackOff(2000L, 2L);
        
        return new org.springframework.kafka.listener.DefaultErrorHandler(recoverer, backOff);
    }
}
