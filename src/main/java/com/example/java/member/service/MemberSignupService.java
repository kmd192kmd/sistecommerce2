package com.example.java.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.dto.MemberDto;
import com.example.java.member.entity.Member;
import com.example.java.member.entity.Memberships;
import com.example.java.member.entity.NotificationPreference;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.repository.MembershipsRepository;
import com.example.java.member.repository.NotificationPreferenceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberSignupService {

    private final MemberRepository memberRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final MembershipsRepository membershipsRepository;

    @Transactional
    public boolean signup(MemberDto memberDto) {

        // 1. 기본값 세팅 후 Member 저장
        MemberDto dto = memberDto.toBuilder()
                .status(1)
                .role("ROLE_USER")
                .loginType("LOCAL")
                .build();

        Member member = dto.toEntity();
        Member savedMember = memberRepository.save(member);

        // 2. 마케팅 동의 여부에 따라 notification_preferences 저장
        String marketingYn = Boolean.TRUE.equals(memberDto.getMarketing()) ? "Y" : "N";

        NotificationPreference pref = NotificationPreference.builder()
                .memberSeq(savedMember)
                .emailYn(marketingYn)
                .smsYn(marketingYn)
                .pushYn(marketingYn)
                .marketingEmailYn(marketingYn)
                .marketingSmsYn(marketingYn)
                .build();

        notificationPreferenceRepository.save(pref);
        
        // 3. 멤버십 테이블 생성 (미가입 상태)
        Memberships memberships = Memberships.builder()
        		.memberSeq(savedMember)
        		.status("none")
        		.build();
        
        membershipsRepository.save(memberships);
        

        return true;
    }
}
