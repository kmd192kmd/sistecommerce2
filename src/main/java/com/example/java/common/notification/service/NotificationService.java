package com.example.java.common.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.common.notification.dto.NotificationResponse;
import com.example.java.common.notification.entity.Notification;
import com.example.java.common.notification.entity.NotificationRecipientType;
import com.example.java.common.notification.entity.NotificationReferenceType;
import com.example.java.common.notification.entity.NotificationType;
import com.example.java.common.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 적재 서비스. 도메인 이벤트의 후처리(리스너)에서만 호출하고,
 * 비즈니스 트랜잭션 안에서 직접 부르지 않는다 — 알림 적재가 비즈 로직(결제/환불)과
 * 운명을 같이 하지 않도록 분리하기 위함(리스너가 AFTER_COMMIT + REQUIRES_NEW로 호출).
 *
 * 메서드는 엔티티가 아니라 (memberSeq, participationSeq) 값만 받는다.
 * 이벤트가 커밋 경계를 넘어 비동기/후처리로 들어오므로, detached 엔티티의 lazy 접근을 피한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationEmitterService emitterService;

    public Notification notifyGroupBuyPromoted(Long memberSeq, Long participationSeq) {
        return save(NotificationType.GROUP_BUY_PROMOTED, "공구 대기열 승격 안내",
                "대기열에서 승격됐어요. 기한 내 결제해 주세요.", memberSeq, participationSeq);
    }

    public Notification notifyGroupBuyConfirmed(Long memberSeq, Long participationSeq) {
        return save(NotificationType.GROUP_BUY_CONFIRMED, "공동구매 확정 안내",
                "참여하신 공동구매가 확정되었어요. 마감 후 순차 배송됩니다.", memberSeq, participationSeq);
    }

    public Notification notifyGroupBuyFailed(Long memberSeq, Long participationSeq) {
        return save(NotificationType.GROUP_BUY_FAILED, "공동구매 무산 안내",
                "참여하신 공동구매가 최소 인원 미달로 무산되었어요.", memberSeq, participationSeq);
    }

    public Notification notifyPaymentDone(Long memberSeq, Long participationSeq) {
        return save(NotificationType.PAYMENT_DONE, "공동구매 결제 완료",
                "공동구매 결제가 완료되어 참여 중 상태로 변경되었습니다.", memberSeq, participationSeq);
    }

    public Notification notifyRefundDone(Long memberSeq, Long participationSeq) {
        return save(NotificationType.REFUND_DONE, "환불 완료 안내",
                "공동구매 결제 취소(환불)가 완료되었습니다.", memberSeq, participationSeq);
    }

    public Notification notifyRefundFailed(Long memberSeq, Long participationSeq) {
        return save(NotificationType.REFUND_FAILED, "환불 실패 안내",
                "공동구매 환불 처리 중 오류가 발생했습니다. 고객센터로 문의해 주세요.", memberSeq, participationSeq);
    }

    // ===================== 회원 조회/읽음 처리 =====================

    /** 회원 알림 목록 (최신순). */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMemberNotifications(Long memberSeq) {
        return notificationRepository
                .findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(
                        NotificationRecipientType.MEMBER, memberSeq)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** 회원 안 읽은 알림 개수 (벨 배지용). */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberSeq) {
        return notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, memberSeq);
    }

    /**
     * 알림 1건 읽음 처리. 본인 알림이 아니면 무시(소유권 검증).
     * 멱등: 이미 읽음이면 readAt을 덮어쓰지 않는다(entity.markRead가 보장).
     */
    @Transactional
    public void markRead(Long memberSeq, Long notificationSeq) {
        notificationRepository.findById(notificationSeq)
                .filter(n -> n.getRecipientType() == NotificationRecipientType.MEMBER
                        && memberSeq.equals(n.getRecipientSeq()))
                .ifPresent(n -> n.markRead(LocalDateTime.now()));
    }

    /** 회원의 안 읽은 알림 전체 읽음 처리. */
    @Transactional
    public void markAllRead(Long memberSeq) {
        LocalDateTime now = LocalDateTime.now();
        notificationRepository
                .findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(
                        NotificationRecipientType.MEMBER, memberSeq)
                .forEach(n -> n.markRead(now));
    }

    private Notification save(NotificationType type, String title, String content,
                              Long memberSeq, Long participationSeq) {
        Notification saved = notificationRepository.save(Notification.builder()
                .type(type)
                .title(title)
                .content(content)
                .recipientType(NotificationRecipientType.MEMBER)
                .recipientSeq(memberSeq)
                .referenceType(NotificationReferenceType.PARTICIPATION)
                .referenceSeq(participationSeq)
                .build());

        // 실시간 푸시(best-effort). 전송 실패해도 알림은 이미 저장됐으므로 삼킨다.
        try {
            emitterService.push(memberSeq, NotificationResponse.from(saved));
        } catch (Exception e) {
            log.warn("[알림 SSE 푸시 실패] memberSeq={} notificationSeq={}", memberSeq, saved.getSeq(), e);
        }
        return saved;
    }
}
