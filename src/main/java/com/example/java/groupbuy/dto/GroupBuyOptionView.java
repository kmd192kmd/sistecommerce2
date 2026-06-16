package com.example.java.groupbuy.dto;

import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.product.entity.Options;

import lombok.Builder;
import lombok.Getter;

/**
 * 회원 노출용 옵션 뷰.
 * 회원에겐 옵션의 '표시명(색상/사이즈 등)'과 '매진 여부'만 공개하고,
 * order_qty / occupied_count(잔여 수량)는 비공개.
 * 따라서 내부용 GroupBuyOptionsDto와 분리한다.
 */
@Getter
@Builder
public class GroupBuyOptionView {

    // 참여(participate)에 보낼 옵션 식별자 = group_buy_options.seq.
    // ⚠️ 상품옵션(options.seq)이 아니다 — participate가 
	// group_buy_options 행을 이 값으로 조회하므로
    // 여기에 상품옵션 seq를 담으면 "옵션을 찾을 수 없습니다"로 참여가 실패한다.
    private Long optionsSeq;
    private String label;       // 옵션 표시명 (예: "블랙", "270", "그레이") — Options.getDisplayName()
    private boolean soldOut;
    private Integer finalPrice;  // 옵션별 실제 공구가 = 공구 기준 할인가 + 그 옵션의 additional_price

    /**
     * @param entity         공구 옵션(group_buy_options)
     * @param baseFinalPrice 공구 기준 할인가(group_buy.final_price). 여기에 옵션 추가금을 더해 옵션별 가격을 만든다.
     */
    public static GroupBuyOptionView from(GroupBuyOptions entity, int baseFinalPrice) {
        Options opt = entity.getOptions();
        // 점유(확정+결제대기) 인원이 발주가능수량에 도달하면 매진
        boolean soldOut = entity.getOccupiedCount() >= entity.getOrderQty();
        // 옵션 추가금(0 이상). 제일 싼 옵션은 0, 비싼 옵션은 양수.
        int additional = (opt != null && opt.getAdditionalPrice() != null) ? opt.getAdditionalPrice() : 0;
        return GroupBuyOptionView.builder()
                // ★ group_buy_options.seq (참여 식별자). 상품옵션 seq(opt.getSeq())가 아님에 주의.
                .optionsSeq(entity.getSeq())
                // 색상/사이즈 등 flat 컬럼 중 값이 있는 것들을 " / "로 조합한 표시명
                .label(opt != null ? opt.getDisplayName() : null)
                .soldOut(soldOut)
                .finalPrice(baseFinalPrice + additional)  // 공구 기준가 + 옵션 추가금
                .build();
    }
}
