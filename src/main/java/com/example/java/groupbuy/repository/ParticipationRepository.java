package com.example.java.groupbuy.repository;

import java.util.Collection;
import java.util.List;
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
}
