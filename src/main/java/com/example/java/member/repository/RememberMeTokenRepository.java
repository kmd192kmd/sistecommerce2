package com.example.java.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.member.entity.RememberMeToken;

public interface RememberMeTokenRepository extends JpaRepository<RememberMeToken, Long> {

    /** 쿠키로 들어온 토큰 해시로 단건 조회 */
    Optional<RememberMeToken> findByTokenHash(String tokenHash);

    /** 특정 회원의 모든 토큰 조회 */
    List<RememberMeToken> findByMember_Seq(Long memberSeq);

    /** 특정 회원의 모든 토큰 삭제 (로그아웃·비밀번호 변경·탈취 의심 시 전체 무효화) */
    @Modifying
    @Query("delete from RememberMeToken t where t.member.seq = :memberSeq")
    int deleteByMemberSeq(@Param("memberSeq") Long memberSeq);

    /** 만료되었거나 이미 사용된 토큰 정리 (스케줄러용) */
    @Modifying
    @Query("delete from RememberMeToken t where t.expireAt < :now or t.usedYn = 'Y'")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);
}
