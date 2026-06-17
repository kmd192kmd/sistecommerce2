package com.example.java.admin.repository;

import com.example.java.member.entity.Member;
import com.example.java.member.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminMemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    // 마법의 메서드 이름 규칙!
    // "findBy + Member + Seq" 라고 이름만 지어주면,
    // JPA가 알아서 "SELECT * FROM member_coupon WHERE member_seq = ?" 쿼리를 짜줍니다.
    List<MemberCoupon> findByMemberSeq_Seq(Member member);  // memberSeq.seq로 조회

}