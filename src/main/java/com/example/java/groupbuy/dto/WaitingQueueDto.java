package com.example.java.groupbuy.dto;

import java.time.LocalDateTime;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.WaitingQueue;
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
public class WaitingQueueDto {

    private Long seq;
    private Long groupBuySeq;
    private Long groupBuyOptionsSeq;
    private Long memberSeq;
    private LocalDateTime createdAt;

    // 엔티티를 Dto로 변환하는 메서드
    public static WaitingQueueDto toDto(WaitingQueue entity) {
        if (entity == null) {
            return null;
        }
        return WaitingQueueDto.builder()
                .seq(entity.getSeq())
                .groupBuySeq(entity.getGroupBuy() != null ? entity.getGroupBuy().getSeq() : null)
                .groupBuyOptionsSeq(entity.getGroupBuyOptions() != null ? entity.getGroupBuyOptions().getSeq() : null)
                .memberSeq(entity.getMemberSeq())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Dto를 엔티티로 변환하는 메서드 
    // 연관관계 엔티티인 GroupBuy와 GroupBuyOptions를 주입받아 매핑
    public WaitingQueue toEntity(GroupBuy groupBuy, GroupBuyOptions groupBuyOptions) {
        return WaitingQueue.builder()
                .seq(this.seq)
                .groupBuy(groupBuy)
                .groupBuyOptions(groupBuyOptions)
                .memberSeq(this.memberSeq)
                .createdAt(this.createdAt)
                .build();
    }
}
