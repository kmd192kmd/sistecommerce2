package com.example.java.groupbuy.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.WaitingQueue;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {
    List<WaitingQueue> findByGroupBuyOrderByCreatedAtAsc(GroupBuy groupBuy);
    List<WaitingQueue> findByGroupBuyOptionsOrderByCreatedAtAsc(GroupBuyOptions groupBuyOptions);
    List<WaitingQueue> findByGroupBuySeqAndMemberSeq(Long groupBuySeq, Long memberSeq);

    /**
     * 이 회원이 해당 공구의 대기열에 이미 등록돼 있는지 (공구 단위).
     *
     * 대기열 등록 시 중복 등록을 막는 용도.
     * DB UNIQUE 제약은 (group_buy_options_seq, member_seq) = "옵션 단위"라
     * 같은 공구의 '다른 옵션' 대기열에는 중복으로 들어갈 수 있다.
     * 하지만 공구는 1인 1상품 원칙이므로, 서비스에서는 '공구 단위'로 막아야 한다.
     * → 그래서 옵션이 아닌 group_buy_seq 기준으로 존재 여부를 확인한다.
     */
    boolean existsByGroupBuySeqAndMemberSeq(Long groupBuySeq, Long memberSeq);

    /**
     * 특정 옵션 대기열에서 가장 먼저 등록된(created_at 최소) 1명을 조회한다 (FIFO 승격 대상).
     * findFirst ... OrderByCreatedAtAsc → created_at 오름차순 첫 행 = 제일 오래 기다린 사람
     * 정규 참여자가 이탈해 자리가 났을 때 이 사람을 승격시킨다 (NFR-002 공정성: 옵션별 FIFO).
     * 대기열이 비어있으면 Optional.empty (승격할 사람 없음 → 자리만 비움).
     */
    Optional<WaitingQueue> findFirstByGroupBuyOptionsOrderByCreatedAtAsc(GroupBuyOptions groupBuyOptions);
}
