package com.example.java.groupbuy.dto;

import java.time.LocalDateTime;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
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
public class ParticipationDto {

    private Long seq;
    private Long groupBuySeq;
    private Long groupBuyOptionsSeq;
    private Long memberSeq;
    private ParticipationStatus status;
    private LocalDateTime paymentDeadline;
    private LocalDateTime promotedAt;
    private LocalDateTime createdAt;

    // 엔티티를 Dto로 변환하는 메서드
    public static ParticipationDto toDto(Participation entity) {
        if (entity == null) {
            return null;
        }
        return ParticipationDto.builder()
                .seq(entity.getSeq())
                .groupBuySeq(entity.getGroupBuy() != null ? entity.getGroupBuy().getSeq() : null)
                .groupBuyOptionsSeq(entity.getGroupBuyOptions() != null ? entity.getGroupBuyOptions().getSeq() : null)
                .memberSeq(entity.getMemberSeq())
                .status(entity.getStatus())
                .paymentDeadline(entity.getPaymentDeadline())
                .promotedAt(entity.getPromotedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Dto를 엔티티로 변환하는 메서드
    // 연관관계 엔티티인 GroupBuy와 GroupBuyOptions를 주입받아 매핑
    public Participation toEntity(GroupBuy groupBuy, GroupBuyOptions groupBuyOptions) {
        return Participation.builder()
                .seq(this.seq)
                .groupBuy(groupBuy)
                .groupBuyOptions(groupBuyOptions)
                .memberSeq(this.memberSeq)
                .status(this.status)
                .paymentDeadline(this.paymentDeadline)
                .promotedAt(this.promotedAt)
                .createdAt(this.createdAt)
                .build();
    }
}
