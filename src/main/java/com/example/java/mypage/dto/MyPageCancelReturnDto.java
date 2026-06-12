package com.example.java.mypage.dto;

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
public class MyPageCancelReturnDto {
    private Long orderItemSeq;
    private Long orderSeq;
    private String orderUid;
    private String orderDate;
    private String productName;
    private Integer productPrice;
    private Integer quantity;
    private Integer itemStatus;
    private String thumbnailUrl;
    
    // CANCEL or RETURN
    private String type; 
    private String statusText;
    
    // 접수일자, 접수번호, 완료일자, 사유
    private String requestDate;
    private String uid;
    private String completedDate;
    private String reason;
    
    // 환불 정보
    private Integer refundPrice;
    private String paymentMethod;
    private Integer originalPrice;
    private Integer discountPrice;
    private Integer deliveryFee;
}
