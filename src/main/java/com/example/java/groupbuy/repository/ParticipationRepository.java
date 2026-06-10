package com.example.java.groupbuy.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByGroupBuy(GroupBuy groupBuy);
    List<Participation> findByGroupBuySeq(Long groupBuySeq);
    List<Participation> findByMemberSeq(Long memberSeq);
    List<Participation> findByGroupBuySeqAndMemberSeq(Long groupBuySeq, Long memberSeq);

    /**
     * 같은 공구에 해당 회원의 참여가 주어진 상태들 중 하나로 존재하는지.
     * 중복참여 검사용 — 진행 중(PARTICIPATING/PAYMENT_PENDING) 참여가 있으면 재참여를 막는다.
     *
     * Spring Data JPA는 '메서드 이름'을 분석해 SQL을 자동으로 만들어준다(직접 쿼리 안 짜도 됨):
     *   exists            → 존재 여부를 boolean으로 반환
     *   By GroupBuySeq    → WHERE group_buy_seq = ?
     *   And MemberSeq     → AND member_seq = ?
     *   And StatusIn      → AND status IN (?, ?, ...)   ← 여러 상태를 한 번에 비교
     * 그래서 "이 회원이 이 공구에 활성 상태로 이미 있나?"를 한 줄로 물어볼 수 있다.
     */
    boolean existsByGroupBuySeqAndMemberSeqAndStatusIn(
            Long groupBuySeq, Long memberSeq, Collection<ParticipationStatus> statuses);

    /**
     * 특정 공구에서 주어진 상태인 참여가 몇 건인지 센다 (화면의 "N명 참여" 집계용).
     *   count By GroupBuySeq And Status → SELECT COUNT(*) ... WHERE group_buy_seq=? AND status=?
     * 예: status=PARTICIPATING 으로 호출하면 현재 정규 참여 인원이 나온다.
     */
    long countByGroupBuySeqAndStatus(Long groupBuySeq, ParticipationStatus status);

    /**
     * 같은 공구에서 해당 회원의 특정 상태 참여 1건을 조회한다 (취소 대상 조회용).
     * 1인 1상품 원칙 + 활성 참여 UNIQUE 제약 덕에 
     * (공구, 회원, PARTICIPATING)이면 최대 1건이라 First로 받는다.
     * findFirst ... AndStatus → status=PARTICIPATING 으로 
     * 호출하면 "취소 가능한 정규 참여"가 나온다.
     * 없으면(이미 취소됐거나 참여한 적 없음) Optional.empty.
     */
    Optional<Participation> findFirstByGroupBuySeqAndMemberSeqAndStatus(
            Long groupBuySeq, Long memberSeq, ParticipationStatus status);

    /**
     * 결제기한이 지난 결제대기(PAYMENT_PENDING) 참여 목록 (만료 처리 스케줄러용).
     *   By Status               → WHERE status = ?            (PAYMENT_PENDING)
     *   And PaymentDeadlineBefore → AND payment_deadline < ?   (기준 시각보다 이전 = 기한 지남)
     * 스케줄러가 이 목록을 받아 각 건을 개별 트랜잭션(expirePromotion)으로 만료 처리한다.
     */
    List<Participation> findByStatusAndPaymentDeadlineBefore(
            ParticipationStatus status, LocalDateTime deadline);
}
