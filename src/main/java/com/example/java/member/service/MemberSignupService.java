package com.example.java.member.service;

import com.example.java.member.dto.MemberDto;
import com.example.java.member.entity.*;
import com.example.java.member.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberSignupService {

    private final MemberRepository memberRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final MembershipsRepository membershipsRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public boolean signup(MemberDto memberDto) {
        Member savedMember = saveMember(memberDto);
        saveNotificationPreference(savedMember, memberDto.getMarketing());
        saveMemberships(savedMember);
        saveMemberCoupon(savedMember);
        return true;
    }
    
    // 1. 기본값 세팅 후 Member 저장
    private Member saveMember(MemberDto memberDto) {
    	Member member = memberDto.toBuilder()
    			.status(1)
                .role("ROLE_USER")
                .loginType("LOCAL")
                .password(passwordEncoder.encode(memberDto.getPassword()))
                .joinedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
                .toEntity();
    	return memberRepository.save(member);
    }
    
    // 2. 마케팅 동의 여부에 따라 notification_preferences 저장
    private void saveNotificationPreference(Member savedMember, Boolean marketing) {
        String yn = Boolean.TRUE.equals(marketing) ? "Y" : "N";
        notificationPreferenceRepository.save(NotificationPreference.builder()
                .member(savedMember)
                .emailYn(yn).smsYn(yn).pushYn(yn)
                .marketingEmailYn(yn).marketingSmsYn(yn)
                .build());
    }
    
    // 3. 멤버십 테이블 생성 (미가입 상태)
    private void saveMemberships(Member savedMember) {
        membershipsRepository.save(Memberships.builder()
                .member(savedMember)
                .status("NONE")
                .build());
    }

    // 4. 신규 쿠폰 발급
    private void saveMemberCoupon(Member savedMember) {
        Coupon coupon = couponRepository.getReferenceById(1L);
        memberCouponRepository.save(MemberCoupon.builder()
                .member(savedMember)
                .coupon(coupon)
                .status(0)
                .build());
    }

    
}
