package com.example.java.groupbuy.payment;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 실제 토스 환불 구현 전까지 사용하는 임시 환불 어댑터. 실제 PG 취소 없이 로그만 남기고 성공으로 간주한다.
 * 공구 취소/마감/강제중단 로직을 실 PG 연동과 무관하게 개발·테스트하기 위한 스텁.
 */
@Slf4j
@Component
public class StubPaymentAdapter implements GroupBuyPaymentPort {

    @Override
    public void refund(Long participationSeq) {
        // 실제 PG 환불 대신 로그만 남기고 성공으로 간주한다.
        log.info("[StubPayment] 공구 참여 환불 처리(스텁) participationSeq={}", participationSeq);
    }
}
