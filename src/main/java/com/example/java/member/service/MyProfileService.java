package com.example.java.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.dto.MemberDto;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.mypage.dto.MyProfileUpdateDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyProfileService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MemberDto getMemberInfo(Long memberSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return MemberDto.from(member);
    }

    @Transactional
    public void updateProfile(Long memberSeq, MyProfileUpdateDto dto) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 닉네임 중복 체크 (본인 제외)
        if (!member.getNickname().equals(dto.getNickname())) {
            boolean nicknameExists = memberRepository.existsByNickname(dto.getNickname());
            if (nicknameExists) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        }

        member.updateProfile(dto.getNickname(), dto.getEmail(), dto.getPhone());
    }
}
