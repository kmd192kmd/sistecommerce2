package com.example.java.groupbuy.dto;

import com.example.java.orders.dto.OrderCreateResultDto;

/**
 * 공구 참여 신청 결과 (토스 2단계 결제 흐름).
 *
 * participate()는 더 이상 즉시 결제를 끝내지 않는다. 옵션 매진 여부로 갈린다:
 *  - QUEUED       : 매진 → 대기열 등록 (결제 정보 없음)
 *  - PARTICIPATED : 정원 확보 → '결제 대기' 주문을 만들고 자리를 예약(occupy)한 상태.
 *                   orderUid/amount로 프론트가 토스 결제창을 띄우고, 결제 성공 시
 *                   confirmPayment→이벤트로 PARTICIPATING이 확정된다. 미결제 시 만료 스케줄러가 자리 반납.
 */
public record ParticipateResponse(
        ParticipateResult result,
        Long ordersSeq,
        String orderUid,
        Integer amount,
        String orderName
) {

    /** 매진 → 대기열 등록 (결제 정보 없음). */
    public static ParticipateResponse queued() {
        return new ParticipateResponse(ParticipateResult.QUEUED, null, null, null, null);
    }

    /** 정원 확보 → 결제 대기 주문 생성. 프론트가 orderUid/amount로 토스 결제창을 띄운다. */
    public static ParticipateResponse paymentRequired(OrderCreateResultDto order) {
        return new ParticipateResponse(
                ParticipateResult.PARTICIPATED,
                order.ordersSeq(),
                order.orderUid(),
                order.finalPrice(),
                order.orderName());
    }
}
