package com.example.java.orders.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderItemCancelRequestDto {

    /**
     * 부분취소할 order_item.seq 목록.
     */
    private List<Long> orderItemSeqList;

    private String cancelReason;
}