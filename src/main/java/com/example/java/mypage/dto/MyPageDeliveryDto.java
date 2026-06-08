package com.example.java.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageDeliveryDto {
    private String companyName;
    private String deliveryStatus;
    private String trackingNumber;
    private String completedAt;
    private List<MyPageOrderItemDto> items;
}
