package com.example.java.groupbuy.payment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.java.config.KafkaConfig;
import com.example.java.groupbuy.service.GroupBuyService;
import com.example.java.orders.event.OrderPaidEvent;
import com.example.java.orders.event.OrderPaymentFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final GroupBuyService groupBuyService;

    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCESS_TOPIC, groupId = "groupbuy-group")
    public void handlePaymentSuccess(OrderPaidEvent event) {
        log.info("Received Kafka OrderPaidEvent: {}", event);
        if (event.participationSeqs() != null) {
            event.participationSeqs().forEach(seq -> {
                try {
                    groupBuyService.confirmAfterPayment(seq);
                    log.info("Successfully confirmed group buy participation seq={}", seq);
                } catch (Exception e) {
                    log.error("Failed to confirm group buy participation seq={}: ", seq, e);
                }
            });
        }
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC, groupId = "groupbuy-group")
    public void handlePaymentFailed(OrderPaymentFailedEvent event) {
        log.info("Received Kafka OrderPaymentFailedEvent: {}", event);
        if (event.participationSeqs() != null) {
            event.participationSeqs().forEach(seq -> {
                try {
                    groupBuyService.cancelPendingPayment(seq);
                    log.info("Successfully cancelled pending group buy participation seq={}", seq);
                } catch (Exception e) {
                    log.error("Failed to cancel pending group buy participation seq={}: ", seq, e);
                }
            });
        }
    }
}
