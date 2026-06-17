package com.example.java.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.MemberStatusLog;

public interface MemberStatusLogRepository extends JpaRepository<MemberStatusLog, Long> {

    /** 특정 회원의 상태 전환 이력 (최신순) */
    List<MemberStatusLog> findByMember_SeqOrderByChangedAtDesc(Long memberSeq);
}
