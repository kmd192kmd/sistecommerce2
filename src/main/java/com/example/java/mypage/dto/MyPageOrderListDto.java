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
public class MyPageOrderListDto {
    private Long orderSeq;
    private String orderDate;
    private List<MyPageDeliveryDto> deliveries;
    private boolean allDelivered;

    public boolean getAllDelivered() {
        return this.allDelivered;
    }

    public boolean isAllDelivered() {
        return this.allDelivered;
    }
}
