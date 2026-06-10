package com.example.java.admin.dto;

import com.example.java.orders.entity.Orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderSummaryDto {
    private Orders order;
    private String representativeItemName;
    private String memberName;
}
