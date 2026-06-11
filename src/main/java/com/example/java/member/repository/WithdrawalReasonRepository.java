package com.example.java.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.WithdrawalReason;

public interface WithdrawalReasonRepository extends JpaRepository<WithdrawalReason, Long> {

    /** 탈퇴 사유 목록 (seq 오름차순) — 신청 화면 표시용 */
    List<WithdrawalReason> findAllByOrderBySeqAsc();
}
