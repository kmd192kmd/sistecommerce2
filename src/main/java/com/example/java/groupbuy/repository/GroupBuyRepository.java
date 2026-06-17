package com.example.java.groupbuy.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyStatus;

public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {

    /**
     * 마감 시각이 지난 진행 중(ONGOING) 공구 목록 (마감 처리 스케줄러용).
     *   By Status         → WHERE status = ?       (ONGOING)
     *   And EndAtBefore    → AND end_at < ?         (마감 시각 지남)
     * 스케줄러가 이 목록을 받아 각 공구를 개별 트랜잭션(close)으로 확정/무산 판정한다.
     */
    List<GroupBuy> findByStatusAndEndAtBefore(GroupBuyStatus status, LocalDateTime endAt);

    /**
     * 시작 시각이 지난 시작 전(SCHEDULED) 공구 목록 (시작 전이 스케줄러용).
     *   By Status          → WHERE status = ?     (SCHEDULED)
     *   And StartAtBefore   → AND start_at < ?     (시작 시각 도래)
     */
    List<GroupBuy> findByStatusAndStartAtBefore(GroupBuyStatus status, LocalDateTime startAt);

    /** 진행 중 공구 목록 (마감 임박순) — 메인 공구 목록 화면(GB-01)용. */
    List<GroupBuy> findByStatusOrderByEndAtAsc(GroupBuyStatus status);

    /**
     * 공구 예정 화면용: 시작 시각이 cutoff 이전(노출 윈도우 내)인 SCHEDULED 공구, 시작 임박순.
     * cutoff = now + N일 (일반 7일 / 멤버십 14일) → 멤버십이 더 일찍 본다.
     */
    List<GroupBuy> findByStatusAndStartAtBeforeOrderByStartAtAsc(GroupBuyStatus status, LocalDateTime cutoff);
}
