package com.example.java.mypage.dto;

import java.util.List;
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
public class MyPageOrderDetailDto {
    private Long orderSeq;
    private String orderUid;
    private String orderDate;
    
    // 받는 사람 배송지 정보
    private String recipientName;
    private String recipientPhone;
    private String zipcode;
    private String address;
    private String addressDetail;
    private String requestMemo;
    
    // 결제 정산 정보
    private String paymentMethod;
    private Integer productTotalPrice;
    private Integer discountPrice;
    private Integer deliveryFee;
    private Integer finalPrice;
    
    // 배송 묶음 및 상품 목록
    private List<MyPageDeliveryDto> deliveries;
}
