package com.example.java.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.java.member.entity.Member;
import com.example.java.admin.repository.AdminMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
    
    private final AdminMemberRepository adminMemberRepository;

    public Page<Member> getMembers(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return adminMemberRepository.findByUsernameContainingOrNameContainingOrNicknameContaining(
                    keyword, keyword, keyword, pageable);
        }
        return adminMemberRepository.findAll(pageable);
    }

    public Member getMember(Long seq) {
        return adminMemberRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateMemberStatus(Long seq, Integer status) {
        Member member = getMember(seq);
        member.changeStatus(status);
    }
}