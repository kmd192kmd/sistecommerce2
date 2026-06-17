package com.example.java.admin.dto;

import java.time.LocalDateTime;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import lombok.Data;

@Data
public class GroupBuySearchDto {
    private GroupBuyStatus status;
    private Integer participantMin;
    private Integer participantMax;
    private LocalDateTime startAtFrom;
    private LocalDateTime startAtTo;
    private LocalDateTime endAtFrom;
    private LocalDateTime endAtTo;
    private String searchType; // "PRODUCT_NAME" (상품명), "SELLER_NAME" (판매처)
    private String keyword;
}
