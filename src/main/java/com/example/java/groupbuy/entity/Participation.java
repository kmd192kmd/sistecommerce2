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
    
    // group_buy ~ participation
    // 공동구매 도메인 내의 엔티티이므로 안전하게 연관관계 매핑 적용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_seq", nullable = false)
    private GroupBuy groupBuy;
    
    // group_buy_options ~ participation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_options_seq", nullable = false)
    private GroupBuyOptions groupBuyOptions;

    // member ~ participation
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
}
