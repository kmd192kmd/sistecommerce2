package com.example.java.chat.repository;

import com.example.java.chat.entity.CsTicket;
import com.example.java.chat.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CsTicketRepository extends JpaRepository<CsTicket, Long> {
    // 특정 채팅에 연결된 티켓 조회
    Optional<CsTicket> findByChatSeq(Long chatSeq);

    // 관리자 대시보드용: 상태별(예: 대기 중) 티켓 목록 조회 (오래된 순으로 처리하기 위해 Asc)
    List<CsTicket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
}