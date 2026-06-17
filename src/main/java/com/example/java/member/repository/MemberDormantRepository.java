package com.example.java.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.MemberDormant;

public interface MemberDormantRepository extends JpaRepository<MemberDormant, Long> {

    /** 특정 회원의 휴면 분리보관 레코드(복원용). 동시에 1건만 존재 가정. */
    Optional<MemberDormant> findFirstByMember_SeqOrderBySeqDesc(Long memberSeq);
}
