package com.example.java.member.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.MemberWithdrawal;

public interface MemberWithdrawalRepository extends JpaRepository<MemberWithdrawal, Long> {

    /** 특정 회원의 진행 중(보류) 탈퇴 신청 1건 — 복구 처리용 */
    Optional<MemberWithdrawal> findFirstByMember_SeqAndWithdrawalYnOrderBySeqDesc(Long memberSeq, String withdrawalYn);

    /**
     * 스케줄러 대상 조회.
     * - 확정 마스킹: withdrawalYn='N' && scheduledDeleteAt <= today (유예 만료)
     * - 최종 파기 : withdrawalYn='Y' && scheduledDeleteAt <= today (5년 경과)
     */
    List<MemberWithdrawal> findByWithdrawalYnAndScheduledDeleteAtLessThanEqual(String withdrawalYn, LocalDate date);
}
