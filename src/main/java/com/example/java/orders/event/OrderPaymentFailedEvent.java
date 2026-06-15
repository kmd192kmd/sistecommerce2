package com.example.java.orders.event;

import java.util.List;

/**
 * 주문 결제 실패/취소 이벤트. 결제 도메인(PaymentService.markPaymentFail)이 failUrl 콜백 시 발행한다.
 * 결제 도메인은 공구를 직접 알지 않고 "이 참여들의 결제가 실패(취소)됐다"는 사실만 방송한다.
 * 공구 도메인의 리스너가 이를 받아 결제대기 참여를 취소(CANCELLED)하고 점유를 반납한다(디커플링).
 *
 * OrderPaidEvent(결제 완료)와 대칭 구조.
 *
 * @param participationSeqs 결제 실패한 공구 참여 seq 목록 (order_item.participation_seq != null 인 건들)
 */
public record OrderPaymentFailedEvent(List<Long> participationSeqs) {
}
