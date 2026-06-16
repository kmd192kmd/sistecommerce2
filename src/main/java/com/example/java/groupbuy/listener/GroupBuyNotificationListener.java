package com.example.java.groupbuy.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.java.common.notification.service.NotificationService;
import com.example.java.groupbuy.event.GroupBuyConfirmedEvent;
import com.example.java.groupbuy.event.GroupBuyFailedEvent;
import com.example.java.groupbuy.event.GroupBuyPaymentDoneEvent;
import com.example.java.groupbuy.event.GroupBuyPromotedEvent;
import com.example.java.groupbuy.event.GroupBuyRefundFailedEvent;
import com.example.java.groupbuy.event.GroupBuyRefundedEvent;

import lombok.RequiredArgsConstructor;

/**
 * 공구 도메인 이벤트 → 알림 적재 연결.
 *
 * 핵심은 결합 분리다. 알림은 비즈 트랜잭션(결제/환불/마감)이 "성공적으로 커밋된 뒤"에만
 * 적재되어야 한다. 그래서:
 *  - {@code @TransactionalEventListener}(기본 phase = AFTER_COMMIT): 발행한 트랜잭션이
 *    롤백되면 알림도 만들지 않는다(환불 롤백 시 "환불됨" 알림이 새는 사고 방지).
 *  - {@code @Transactional(REQUIRES_NEW)}: 커밋 이후엔 활성 트랜잭션이 없으므로 알림 적재용
 *    새 트랜잭션을 연다. 알림 적재 실패가 비즈 로직에 영향을 주지 않는다.
 *
 * (기존 {@code GroupBuyPaymentConfirmListener}는 원자성이 목적이라 동기 @EventListener를 쓰지만,
 *  알림은 정반대로 "분리"가 목적이라 AFTER_COMMIT을 쓴다.)
 */
@Component
@RequiredArgsConstructor
public class GroupBuyNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPromoted(GroupBuyPromotedEvent e) {
        notificationService.notifyGroupBuyPromoted(e.memberSeq(), e.participationSeq());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onConfirmed(GroupBuyConfirmedEvent e) {
        notificationService.notifyGroupBuyConfirmed(e.memberSeq(), e.participationSeq());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFailed(GroupBuyFailedEvent e) {
        notificationService.notifyGroupBuyFailed(e.memberSeq(), e.participationSeq());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentDone(GroupBuyPaymentDoneEvent e) {
        notificationService.notifyPaymentDone(e.memberSeq(), e.participationSeq());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRefunded(GroupBuyRefundedEvent e) {
        notificationService.notifyRefundDone(e.memberSeq(), e.participationSeq());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRefundFailed(GroupBuyRefundFailedEvent e) {
        notificationService.notifyRefundFailed(e.memberSeq(), e.participationSeq());
    }
}
