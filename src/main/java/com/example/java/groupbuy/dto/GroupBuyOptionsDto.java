package com.example.java.groupbuy.dto;

import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.product.entity.Options;
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
public class GroupBuyOptionsDto {

    private Long seq;
    private Long groupBuySeq;
    private Long optionsSeq;
    private Integer orderQty;
    private Integer occupiedCount;

    // 엔티티를 Dto로 변환하는 메서드
    public static GroupBuyOptionsDto toDto(GroupBuyOptions entity) {
        if (entity == null) {
            return null;
        }
        return GroupBuyOptionsDto.builder()
            .seq(entity.getSeq())
            .groupBuySeq(entity.getGroupBuy() != null ? entity.getGroupBuy().getSeq() : null)
            .optionsSeq(entity.getOptions() != null ? entity.getOptions().getSeq() : null)
            .orderQty(entity.getOrderQty())
            .occupiedCount(entity.getOccupiedCount())
            .build();
    }

    // Dto를 엔티티로 변환하는 메서드
    // 연관관계 엔티티인 GroupBuy, Options를 주입받아 매핑
    public GroupBuyOptions toEntity(GroupBuy groupBuy, Options options) {
        return GroupBuyOptions.builder()
            .seq(this.seq)
            .groupBuy(groupBuy)
            .options(options)
            .orderQty(this.orderQty)
            .occupiedCount(this.occupiedCount)
            .build();
    }
}
