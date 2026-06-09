package com.example.java.member.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.dto.FindUsernameResultDto;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindUsernameService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<FindUsernameResultDto> findByNameAndEmail(String name, String email) {
        List<Member> members = memberRepository.findByNameAndEmail(name, email);
        return members.stream()
                .map(m -> new FindUsernameResultDto(mask(m.getUsername()), m.getJoinedAt()))
                .collect(Collectors.toList());
    }

    /**
     * 아이디 마스킹
     * - 이메일형(@ 포함): 로컬 앞 2자리 + 마스킹 + @ + 도메인
     *   예) hong@naver.com → ho*****@naver.com
     * - 일반형: 앞 3자리 + 마스킹
     *   예) honggildong → hon********
     */
    private String mask(String username) {
        if (username == null || username.isBlank()) return "";

        if (username.contains("@")) {
            int atIdx = username.indexOf("@");
            String local  = username.substring(0, atIdx);
            String domain = username.substring(atIdx);          // "@naver.com"

            int visibleLen = Math.min(2, local.length());
            String masked = local.substring(0, visibleLen)
                    + "*".repeat(local.length() - visibleLen);
            return masked + domain;
        }

        int visibleLen = Math.min(3, username.length());
        return username.substring(0, visibleLen)
                + "*".repeat(username.length() - visibleLen);
    }
}
