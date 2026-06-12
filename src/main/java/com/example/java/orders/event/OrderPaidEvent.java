package com.example.java.orders.event;

import java.util.List;

/**
 * 주문 결제 완료 이벤트. 결제 도메인(PaymentService.confirmPayment)이 결제 확정 후 발행한다.
 * 결제 도메인은 공구를 직접 알지 않고 "이 참여들의 결제가 끝났다"는 사실만 방송한다.
 * 공구 도메인의 리스너가 이를 받아 참여를 확정한다(디커플링).
 *
 * @param participationSeqs 결제된 공구 참여 seq 목록 (order_item.participation_seq != null 인 건들)
 */
public record OrderPaidEvent(List<Long> participationSeqs) {
}
