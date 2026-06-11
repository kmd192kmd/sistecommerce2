package com.example.java.groupbuy.payment;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.java.groupbuy.service.GroupBuyService;
import com.example.java.orders.event.OrderPaidEvent;

import lombok.RequiredArgsConstructor;

/**
 * 결제 완료 → 공구 참여 확정 연결(이벤트 방식).
 *
 * 결제 도메인이 공구를 직접 호출하지 않도록, 결제 완료 사실은 OrderPaidEvent로 방송되고
 * 그 반응(참여 확정)은 공구 도메인인 이 리스너가 맡는다. ("결제가 공구를 주무르는" 게 아니라
 * "공구가 결제 결과에 반응한다" — 책임 분리, 기존 ES 리스너 패턴과 동일.)
 *
 * @EventListener는 동기라 발행 지점(confirmPayment)의 트랜잭션 안에서 실행된다.
 * 따라서 확정이 실패하면 결제까지 함께 롤백돼 "결제완료 = 참여확정"이 원자적으로 보장된다(NFR-003).
 */
@Component
@RequiredArgsConstructor
public class GroupBuyPaymentConfirmListener {

    private final GroupBuyService groupBuyService;

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        event.participationSeqs().forEach(groupBuyService::confirmAfterPayment);
    }
}
