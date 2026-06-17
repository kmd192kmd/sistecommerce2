package com.example.java.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.EmailToken;
import com.example.java.member.entity.Member;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenHash(String tokenHash);

    List<EmailToken> findByMemberAndPurposeAndUsedYn(Member member, String purpose, String usedYn);
}
