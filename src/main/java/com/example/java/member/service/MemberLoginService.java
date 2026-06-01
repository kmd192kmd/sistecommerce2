package com.example.java.member.service;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.member.dto.MemberDto;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberLoginService {

    private final MemberRepository memberRepository;

    public boolean login(String username, String password, HttpSession session) {
        Member member = memberRepository.findByUsername(username)
                .orElse(null);

        if (member == null || !member.getPassword().equals(password)) {
            return false;
        }

        // 비밀번호 제외하고 세션 저장
        MemberDto auth = MemberDto.from(member).toBuilder()
                .password(null)
                .build();

        session.setAttribute("auth", auth);
        return true;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}