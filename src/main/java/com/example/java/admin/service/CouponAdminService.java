package com.example.java.admin.service;

import com.example.java.member.repository.MemberRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.dto.CouponRequestDto;
import com.example.java.admin.dto.MemberSearchDto;
import com.example.java.admin.repository.AdminCouponRepository;
import com.example.java.admin.repository.AdminMemberCouponRepository;
import com.example.java.admin.repository.AdminMemberRepository;
import com.example.java.member.entity.Coupon;
import com.example.java.member.entity.Member;
import com.example.java.member.entity.MemberCoupon;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // final이 붙은 필드의 생성자를 자동으로 만들어줍니다. (의존성 주입)
public class CouponAdminService {

    private final AdminMemberRepository memberRepository;
	// 우리가 조금 전에 만든 텅 빈 인터페이스! 스프링이 알아서 구현체를 주입해 줍니다.
    private final AdminCouponRepository couponRepository;

    private final AdminMemberCouponRepository adminMemberCouponRepository;

    /**
     * 1. 쿠폰 생성 로직
     */
    @Transactional // 데이터베이스에 데이터를 넣거나 수정할 땐 필수! (실패 시 자동 롤백)
    public void createCoupon(CouponRequestDto dto) {
        
        // 1단계: 비즈니스 로직 검증 (DDL 제약조건 CHK_COUPON_DISCOUNT 방어) 
        if (dto.getDiscountPrice() == null && dto.getDiscountRate() == null) {
            throw new IllegalArgumentException("할인 금액이나 할인율 중 하나는 반드시 입력해야 합니다.");
        }

        // 2단계: DTO를 Entity로 변환 (앞서 DTO 클래스에 만들어둔 편의 메서드 활용)
        Coupon coupon = dto.toEntity();

        // 3단계: DB에 저장 (이 한 줄로 INSERT 쿼리가 자동으로 날아갑니다!)
        couponRepository.save(coupon);
    }
    
    
 // CouponAdminService.java 내부에 추가

    /**
     * 2. 전체 쿠폰 목록 조회
     */
    @Transactional(readOnly = true) // 조회만 할 때는 readOnly=true를 주면 성능이 좋아집니다.
    public List<Coupon> getAllCoupons() {
        // DB에 있는 모든 쿠폰을 최신순(seq 내림차순)으로 정렬해서 가져옵니다.
        return couponRepository.findAll(Sort.by(Sort.Direction.DESC, "seq"));
    }
    
    @Transactional(readOnly = true)
    public Page<Member> getMembersByPage(int page) {
        // 한 페이지당 10명씩, seq 내림차순으로 가져오겠다는 설정 (page는 0부터 시작)
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "seq"));
        
        // 10만 명 중 딱 10명만 쿼리해서 가져옵니다!
        return memberRepository.findAll(pageRequest);
    }
    
    @Transactional(readOnly = true)
    public List<MemberSearchDto> searchMembersForCoupon(String keyword) {
        // 인자를 3개 전달해야 합니다.
        List<Member> members = memberRepository.findByUsernameContainingOrNameContainingOrNicknameContaining(keyword,
  keyword, keyword);

        return members.stream()
                .map(m -> new MemberSearchDto(m.getSeq(), m.getName(), m.getUsername()))
                .collect(Collectors.toList());
    }


    @Transactional
    public void issueScheduledCoupons() {
        // 1. 오늘 날짜 구하기
        java.time.LocalDate today = java.time.LocalDate.now();

        // 2. 대기 중인(status=0) 쿠폰 중 시작일이 오늘이거나 이미 지난 쿠폰 조회
        List<Coupon> pendingCoupons = couponRepository.findByStatusAndStartDateLessThanEqual(0, today);

        if (pendingCoupons.isEmpty()) return;

        // 3. 전체 회원 조회
        List<Member> allMembers = memberRepository.findAll();

        // 4. 회원별로 쿠폰 발급 (MemberCoupon에 저장)
        for (Coupon coupon : pendingCoupons) {
            List<MemberCoupon> memberCoupons = allMembers.stream()
                .map(member -> MemberCoupon.builder()
                    .member(member)
                    .coupon(coupon)
                    .status(0) // 지급은 되었으나 아직 사용하지 않은 상태
                    .build())
                .collect(Collectors.toList());

            adminMemberCouponRepository.saveAll(memberCoupons); // DB에 발급 내역 저장
            coupon.updateStatus(1); // 다시 발급되지 않도록 배포 완료(1)로 변경
        }
    }


    @Transactional
    public void issueCouponManual(Long couponSeq, String issueType, String memberSeqs) {

        // 1. 발급할 쿠폰 엔티티를 찾아옵니다.
        Coupon coupon = couponRepository.findById(couponSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 전체 발급(All)인지 특정 회원 발급(SPECIFIC)인지 구별해서 처리
        if ("All".equals(issueType)) {
            // 모든 회원 조회 후 쿠폰 일괄 지급
            List<Member> allMembers = memberRepository.findAll();
            List<MemberCoupon> memberCoupons = allMembers.stream()
                .map(member -> MemberCoupon.builder()
                    .member(member)
                    .coupon(coupon)
                    .status(0) // 미사용 상태(0)
                    .build())
                .collect(Collectors.toList());
            adminMemberCouponRepository.saveAll(memberCoupons);

        } else if ("SPECIFIC".equals(issueType)) {
            // 특정 회원의 경우 폼에서 넘어온 "1,3,5" 같은 번호를 리스트로 변환합니다.
            if (memberSeqs != null && !memberSeqs.isEmpty()) {
                List<Long> seqList = Arrays.stream(memberSeqs.split(","))
                                           .map(Long::parseLong)
                                           .collect(Collectors.toList());

                // 해당 번호의 회원들만 조회
                List<Member> specificMembers = memberRepository.findAllById(seqList);
                List<MemberCoupon> memberCoupons = specificMembers.stream()
                    .map(member -> MemberCoupon.builder()
                        .member(member)
                        .coupon(coupon)
                        .status(0)
                        .build())
                    .collect(Collectors.toList());
                adminMemberCouponRepository.saveAll(memberCoupons);
            }
        }

        // 3. 발급이 완료된 원본 쿠폰의 상태를 '배포 완료(1)'로 변경합니다.
        coupon.updateStatus(1);
    }

	
	@Transactional(readOnly = true)
    public Coupon getCoupon(Long seq) {
        return couponRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }

    @Transactional
    public void updateCoupon(Long seq, com.example.java.admin.dto.CouponRequestDto dto) {
        Coupon coupon = couponRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.update(dto.getName(), dto.getDiscountType(), dto.getDiscountPrice(), dto.getDiscountRate(), dto.
getStartDate(), dto.getValidDays());
    }
    
    
    @Transactional
    public void deleteCoupon(Long seq) {
       /// 1. 삭제할 쿠폰을 먼저 조회합니다.
        Coupon coupon = couponRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

       /// 2. 쿠폰 상태가 배포 대기(0)인지 검증합니다.
        if (coupon.getStatus() != 0) {
            throw new IllegalStateException("배포 대기(상태: 0)인 쿠폰만 삭제할 수 있습니다.");
        }

       /// 3. 검증을 통과했다면 하드 딜리트를 수행합니다.
        couponRepository.deleteById(seq);
    }
    
    // (여기에 나중에 issueCoupon(쿠폰 발급) 로직 등이 추가될 예정입니다)
}