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
public class MyPageOrderListDto {
    private String orderDate;
    private String deliveryStatus;
    private String image;
    private String name;
    private Integer price;
    private Integer qty;
    private String trackingNumber;
    private Long productSeq;
}
