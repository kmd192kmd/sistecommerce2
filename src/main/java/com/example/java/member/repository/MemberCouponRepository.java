package com.example.java.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.MemberCoupon;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    List<MemberCoupon> findByMember_Seq(Long memberSeq);
}
