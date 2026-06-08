package com.example.java.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.java.admin.hotdeal.Entity.HotDeal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotDealRequestDto {

	private Long seq;
	
    @NotBlank(message = "핫딜 이름은 필수입니다.")
    private String name;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDateTime endDate;
    
    private List<Long> optionsSeqs;
    private List<Integer> hotDealStocks;

    // DB 제약조건: 둘 중 하나는 반드시 존재해야 함
    private Integer discountRate;
    private Integer discountPrice;

    /**
     * 핵심 방어선: 할인율(%)이나 할인가격(원) 중 하나는 반드시 입력되었는지 교차 검증합니다.
     */
    @AssertTrue(message = "할인율(%)이나 할인가격(원) 중 하나는 반드시 입력해야 합니다.")
    public boolean isValidDiscount() {
        return this.discountRate != null || this.discountPrice != null;
    }

    /**
     * DTO -> Entity 변환 메서드
     * 관리자가 최초 생성 시 상태는 항상 '0(대기)'로 고정하여 DB에 넘깁니다.
     */
    public HotDeal toEntity() {
        return HotDeal.builder()
                .name(this.name)
                // 💡핵심: 사용자가 30분을 골라도 서버에서 강제로 00분 00초로 잘라버립니다.
                .startDate(this.startDate.withMinute(0).withSecond(0).withNano(0))
                .endDate(this.endDate.withMinute(0).withSecond(0).withNano(0))
                .status(0) // 0: 대기 상태
                .discountRate(this.discountRate)
                .discountPrice(this.discountPrice)
                .build();
    }
    
    

}