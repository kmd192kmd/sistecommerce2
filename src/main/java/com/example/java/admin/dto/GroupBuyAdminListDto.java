package com.example.java.admin.dto;

import java.time.LocalDateTime;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBuyAdminListDto {
    private Long seq;
    private GroupBuyStatus status;
    private String sellerName;
    private String productName;
    private Long participantCount;
    private Integer minCount;
    private Long waitingCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    // 남은 인원 계산 Getter
    public Long getRemainingCount() {
        return Math.max(0L, minCount - (participantCount != null ? participantCount : 0L));
    }
}
