package com.example.java.groupbuy.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participation")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "participation_seq")
    @SequenceGenerator(name = "participation_seq", sequenceName = "participation_seq", allocationSize = 1)
    private Long seq;

    // 우리 도메인 내의 엔티티이므로 안전하게 연관관계 매핑 적용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_seq", nullable = false)
    private GroupBuy groupBuy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_options_seq", nullable = false)
    private GroupBuyOptions groupBuyOptions;

    // 타 도메인 연관관계: 우선 Long ID로 매핑하여 컴파일/JPA 기동 에러 방지
    @Column(name = "member_seq", nullable = false)
    private Long memberSeq;

    /* [연관관계 매핑 전환용 주석] Member 엔티티 활용 시 주석 해제 후 memberSeq 필드 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", insertable = false, updatable = false)
    private com.example.java.member.entity.Member member;
    */

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParticipationStatus status;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 참여 취소: 상태를 CANCELLED로 전이.
     * setter를 열지 않고 의미 있는 도메인 메서드로 상태를 바꾼다(GroupBuyOptions.occupy/release와 동일한 패턴).
     * 영속 상태의 엔티티이므로 트랜잭션 commit 시 변경감지로 UPDATE 된다.
     */
    public void cancel() {
        this.status = ParticipationStatus.CANCELLED;
    }

    /**
     * 승격자 결제 완료: 결제대기(PAYMENT_PENDING) → 참여중(PARTICIPATING)으로 전이.
     * 승격 시 이미 옵션을 점유(occupied_count)한 상태이므로, 이 전이로 점유 수는 변하지 않는다.
     * 다만 확정 인원(PARTICIPATING 수)에 비로소 포함된다 → 결제 완료 = 확정 정합성(NFR-003).
     */
    public void confirmPayment() {
        this.status = ParticipationStatus.PARTICIPATING;
    }

    /**
     * 승격자 결제기한 만료: PAYMENT_PENDING → EXPIRED.
     * 결제기한 내 결제하지 않아 자격이 소멸된 경우(스케줄러가 호출).
     * 점유(occupied_count) 복구와 다음 대기자 승격은 서비스에서 별도로 처리한다.
     * EXPIRED는 활성 참여가 아니므로(중복참여 검사·UNIQUE 제약에서 제외) 같은 공구 재참여가 가능하다.
     */
    public void expire() {
        this.status = ParticipationStatus.EXPIRED;
    }

    /**
     * 마감 확정 시 참여 확정: PARTICIPATING → CONFIRMED.
     * 공구가 최소 인원을 달성해 성사된 경우, 결제 완료한 참여자가 최종 확정된다.
     */
    public void confirm() {
        this.status = ParticipationStatus.CONFIRMED;
    }

    /**
     * 마감 무산 시 참여 무산: PARTICIPATING → FAILED.
     * 공구가 최소 인원 미달로 무산되면 결제 완료자는 무산 처리되고 환불된다(환불은 서비스).
     */
    public void fail() {
        this.status = ParticipationStatus.FAILED;
    }
}
