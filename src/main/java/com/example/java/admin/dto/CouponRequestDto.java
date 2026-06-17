package com.example.java.admin.dto;

import com.example.java.member.entity.Coupon;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponRequestDto {
    
    // DB 매핑 어노테이션이 모두 빠지고 순수한 필드만 남습니다.
    private String name;
    private Integer discountType; 
    private LocalDate startDate;
    private Integer validDays;
    private Integer discountPrice;
    private Integer discountRate;
    private String target;

    // DTO 내부에서 Entity로 변환해주는 편의 메서드 (빌더 패턴 활용)
    public Coupon toEntity() {
        return Coupon.builder()
                .name(this.name)
                .discountType(this.discountType)
                .startDate(this.startDate)
                .validDays(this.validDays)
                .status(0) // 쿠폰 생성 시 기본 상태는 항상 배포 대기(0) [cite: 269]
                .discountPrice(this.discountPrice)
                .discountRate(this.discountRate)
                .build();
    }
}