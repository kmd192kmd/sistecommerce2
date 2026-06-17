package com.example.java.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.Member;
import com.example.java.member.entity.MembershipsLog;

public interface MembershipsLogRepository extends JpaRepository<MembershipsLog, Long> {

    List<MembershipsLog> findByMemberOrderByCreatedAtDesc(Member member);
}
