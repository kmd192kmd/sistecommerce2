package com.example.java.member.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.repository.MemberCouponRepository;
import com.example.java.mypage.dto.MyCouponDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    public List<MyCouponDto> getMyCoupons(Long memberSeq) {
        return memberCouponRepository.findByMember_Seq(memberSeq)
                .stream()
                .map(MyCouponDto::new)
                .collect(Collectors.toList());
    }
}
