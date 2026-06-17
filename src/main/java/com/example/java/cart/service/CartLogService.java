package com.example.java.cart.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.cart.entity.CartLog;
import com.example.java.cart.repository.CartLogRepository;
import com.example.java.member.entity.Member;
import com.example.java.product.entity.Options;

import com.example.java.retail.service.RetailUserEventService;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 행위 로그 서비스.
 *
 * 로그인 회원의 행위만 기록합니다 (cart_log.member_seq NOT NULL).
 *
 * status 값:
 *  0 - 담기 (비로그인 병합 포함)
 *  1 - 삭제
 *  2 - 구매 (주문 완료 시점에 호출)
 */
@Service
@RequiredArgsConstructor
public class CartLogService {

    private final CartLogRepository cartLogRepository;
    private final RetailUserEventService retailUserEventService;

    /** 담기 로그 (로그인 회원 장바구니 추가 / 비로그인 병합 모두 동일하게 기록) */
    @Transactional
    public void logAdd(Member member, Options options) {
        save(member, options, CartLog.STATUS_ADD);
    }

    /** 삭제 로그 */
    @Transactional
    public void logRemove(Member member, Options options) {
        save(member, options, CartLog.STATUS_REMOVE);
    }

    /** 구매 로그 (주문 완료 시점에 호출) */
    @Transactional
    public void logPurchase(Member member, Options options) {
        save(member, options, CartLog.STATUS_PURCHASE);
    }

    private void save(Member member, Options options, String status) {
        cartLogRepository.save(CartLog.builder()
                .member(member)
                .options(options)
                .status(status)
                .actionDate(LocalDateTime.now())
                .build());
                
        // 구글 Retail AI 실시간 행동 로그 전송
        if (member != null && options != null && options.getProduct() != null) {
            String eventType = "detail-page-view";
            if (CartLog.STATUS_ADD.equals(status)) eventType = "add-to-cart";
            else if (CartLog.STATUS_PURCHASE.equals(status)) eventType = "purchase-complete";
            else if (CartLog.STATUS_REMOVE.equals(status)) eventType = "remove-from-cart";

            retailUserEventService.sendUserEvent(
                    member.getSeq(), 
                    options.getProduct().getSeq(), 
                    eventType
            );
        }
    }
}
