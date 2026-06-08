package com.example.java.groupbuy.payment;

/**
 * 공구 참여 결제 경계(포트).
 * 실제 PG 결제 + orders/payment/order_item 생성은 결제팀 담당이라, 공구 참여 로직은 이 포트 뒤로 분리한다.
 * 현재는 {@link StubPaymentAdapter}(항상 성공)로 대체하고, 추후 결제팀 구현체를 주입한다.
 */
public interface GroupBuyPaymentPort {

    /**
     * 공구 참여 결제. 실패 시 예외를 던져 호출측 트랜잭션을 롤백시킨다(점유도 함께 원복).
     *
     * @param memberSeq 결제 회원 seq
     * @param amount    결제 금액 (공구 고정 할인가 final_price)
     */
    void pay(Long memberSeq, int amount);
}
