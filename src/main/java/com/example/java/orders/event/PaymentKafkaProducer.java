package com.example.java.orders.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.java.config.KafkaConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("Publishing OrderPaidEvent to Kafka: {}", event);
        kafkaTemplate.send(KafkaConfig.PAYMENT_SUCCESS_TOPIC, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaymentFailed(OrderPaymentFailedEvent event) {
        log.info("Publishing OrderPaymentFailedEvent to Kafka: {}", event);
        kafkaTemplate.send(KafkaConfig.PAYMENT_FAILED_TOPIC, event);
    }
}
