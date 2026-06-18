package com.example.java.groupbuy.payment;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.java.groupbuy.service.GroupBuyService;
import com.example.java.orders.event.OrderPaymentFailedEvent;

import lombok.RequiredArgsConstructor;

/**
 * 결제 실패(취소) → 공구 결제대기 참여 취소 연결(이벤트 방식). {@link GroupBuyPaymentConfirmListener}와 대칭.
 *
 * 결제 도메인이 공구를 직접 호출하지 않도록, 결제 실패 사실은 OrderPaymentFailedEvent로 방송되고
 * 그 반응(참여 취소 + 점유 반납)은 공구 도메인인 이 리스너가 맡는다.
 *
 * @EventListener는 동기라 발행 지점(markPaymentFail)의 트랜잭션 안에서 실행된다.
 */
// @Component
@RequiredArgsConstructor
public class GroupBuyPaymentFailedListener {

    private final GroupBuyService groupBuyService;

    // @EventListener
    public void onOrderPaymentFailed(OrderPaymentFailedEvent event) {
        event.participationSeqs().forEach(groupBuyService::cancelPendingPayment);
    }
}
