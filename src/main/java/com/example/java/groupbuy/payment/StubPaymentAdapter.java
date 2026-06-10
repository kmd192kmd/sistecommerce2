package com.example.java.groupbuy.payment;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 결제팀 구현 전까지 사용하는 임시 결제 어댑터. 실제 PG 호출 없이 항상 성공으로 처리한다.
 * 참여 로직(점유/참여 INSERT/동시성)을 결제 완성과 무관하게 독립 개발하기 위한 스텁.
 */
@Slf4j
@Component
public class StubPaymentAdapter implements GroupBuyPaymentPort {

    @Override
    public void pay(Long memberSeq, int amount) {
        // 실제 PG 연동 대신 로그만 남기고 성공으로 간주한다.
        log.info("[StubPayment] 공구 참여 결제 성공 처리(스텁) memberSeq={}, amount={}", memberSeq, amount);
    }

    @Override
    public void cancel(Long memberSeq, int amount) {
        // 실제 PG 환불 대신 로그만 남기고 성공으로 간주한다.
        log.info("[StubPayment] 공구 참여 환불 처리(스텁) memberSeq={}, amount={}", memberSeq, amount);
    }
}
