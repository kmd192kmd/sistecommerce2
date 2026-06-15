package com.example.java.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.Member;
import com.example.java.member.entity.Memberships;

public interface MembershipsRepository extends JpaRepository<Memberships, Long> {

    Optional<Memberships> findByMember(Member member);

    Optional<Memberships> findByMemberAndStatus(Member member, String status);

    /** 자동갱신 스케줄러용: 갱신 시각이 지난 active 멤버십 조회 */
    List<Memberships> findByStatusAndNextBillingAtBefore(String status, LocalDateTime now);

    /** 만료 처리용: 만료일이 지난 canceled 멤버십 조회 */
    List<Memberships> findByStatusAndExpireAtBefore(String status, LocalDateTime now);
}
