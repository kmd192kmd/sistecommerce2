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

    private Long optionsSeq;
    private String label;       // 옵션 표시명 (예: "블랙", "270", "그레이") — Options.getDisplayName()
    private boolean soldOut;

    public static GroupBuyOptionView from(GroupBuyOptions entity) {
        Options opt = entity.getOptions();
        // 점유(확정+결제대기) 인원이 발주가능수량에 도달하면 매진
        boolean soldOut = entity.getOccupiedCount() >= entity.getOrderQty();
        return GroupBuyOptionView.builder()
                .optionsSeq(opt != null ? opt.getSeq() : null)
                // 색상/사이즈 등 flat 컬럼 중 값이 있는 것들을 " / "로 조합한 표시명
                .label(opt != null ? opt.getDisplayName() : null)
                .soldOut(soldOut)
                .build();
    }
}
