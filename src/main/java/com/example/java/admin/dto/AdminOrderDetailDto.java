package com.example.java.admin.dto;

import java.util.List;

import com.example.java.member.entity.Member;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDetailDto {
    private Orders order;
    private List<OrderItem> orderItems;
    private Member member;
}
