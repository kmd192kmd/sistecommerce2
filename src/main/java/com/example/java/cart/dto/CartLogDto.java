package com.example.java.cart.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartLogDto {

    private Long seq;

    private Long memberSeq;

    private Long optionsSeq;

    private String status;
    private LocalDateTime actionDate;

}
