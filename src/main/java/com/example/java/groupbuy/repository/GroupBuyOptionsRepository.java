package com.example.java.groupbuy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;

import jakarta.persistence.LockModeType;

public interface GroupBuyOptionsRepository extends JpaRepository<GroupBuyOptions, Long> {
    List<GroupBuyOptions> findByGroupBuy(GroupBuy groupBuy);
    List<GroupBuyOptions> findByGroupBuySeq(Long groupBuySeq);

    /**
     * 참여/취소/승격 시 옵션 행을 비관적 쓰기 락으로 잠그고 조회한다 
     * -> (Oracle: SELECT ... FOR UPDATE).
     * occupied_count 점유/복구 전에 호출해 동시 참여 경쟁을 직렬화한다 (NFR-001).
     * max_count = 옵션 order_qty 합이므로, 
     * 옵션 행 하나만 잠그면 전체 ≤ max_count 도 자동 보장된다.
     * 
     * - 그냥 SELECT = 문 열고 안을 들여다보기만 함 (여러 명이 동시에 가능)
     * - SELECT FOR UPDATE = 들어가서 문을 잠금 
     * → 내가 나올 때까지 다음 사람은 문 앞에서 대기
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from GroupBuyOptions o where o.seq = :seq")
    Optional<GroupBuyOptions> findBySeqForUpdate(@Param("seq") Long seq);
}
