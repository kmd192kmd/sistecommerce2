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
public class MyPageOrderItemDto {
    private Long productSeq;
    private String name;
    private String image;
    private Integer price;
    private Integer qty;
}
