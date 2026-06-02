package com.example.java.groupbuy.dto;

import java.time.LocalDateTime;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyStatus;
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
public class GroupBuyDto {

    private Long seq;
    private Long productSeq;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private Integer minCount;
    private Integer maxCount;
    private Integer originalPrice;
    private Integer finalPrice;
    private GroupBuyStatus status;

    // 엔티티를 Dto로 변환하는 메서드
    public static GroupBuyDto toDto(GroupBuy entity) {
        if (entity == null) {
            return null;
        }
        return GroupBuyDto.builder()
                .seq(entity.getSeq())
                .productSeq(entity.getProductSeq())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .createdAt(entity.getCreatedAt())
                .finishedAt(entity.getFinishedAt())
                .minCount(entity.getMinCount())
                .maxCount(entity.getMaxCount())
                .originalPrice(entity.getOriginalPrice())
                .finalPrice(entity.getFinalPrice())
                .status(entity.getStatus())
                .build();
    }

    // Dto를 엔티티로 변환하는 메서드
    public GroupBuy toEntity() {
        return GroupBuy.builder()
                .seq(this.seq)
                .productSeq(this.productSeq)
                .startAt(this.startAt)
                .endAt(this.endAt)
                .createdAt(this.createdAt)
                .finishedAt(this.finishedAt)
                .minCount(this.minCount)
                .maxCount(this.maxCount)
                .originalPrice(this.originalPrice)
                .finalPrice(this.finalPrice)
                .status(this.status)
                .build();
    }
}
