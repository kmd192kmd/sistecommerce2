package com.example.java.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.java.admin.dto.AdminOrderDetailDto;
import com.example.java.admin.dto.AdminOrderSummaryDto;
import com.example.java.admin.repository.AdminMemberRepository;
import com.example.java.admin.repository.AdminOrderItemRepository;
import com.example.java.admin.repository.AdminOrdersRepository;
import com.example.java.member.entity.Member;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    
    private final AdminOrdersRepository adminOrdersRepository;
    private final AdminOrderItemRepository adminOrderItemRepository;
    private final AdminMemberRepository adminMemberRepository;

    public AdminOrderDetailDto getOrderDetail(String orderUid) {
        Orders order = adminOrdersRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문번호입니다."));
        
        List<OrderItem> items = adminOrderItemRepository.findByOrderSeq(order.getSeq());
        
        Member member = adminMemberRepository.findById(order.getMemberSeq())
                .orElse(null); // 회원이 탈퇴했거나 없을 수도 있으므로 null 허용
        
        return AdminOrderDetailDto.builder()
                .order(order)
                .orderItems(items)
                .member(member)
                .build();
    }

    public Page<AdminOrderSummaryDto> getOrders(String keyword, Pageable pageable) {
        Page<Orders> orderPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            orderPage = adminOrdersRepository.searchByMemberKeyword(keyword.trim(), pageable);
        } else {
            orderPage = adminOrdersRepository.findAll(pageable);
        }

        List<AdminOrderSummaryDto> dtoList = new ArrayList<>();
        for (Orders order : orderPage.getContent()) {
            List<OrderItem> items = adminOrderItemRepository.findByOrderSeq(order.getSeq());
            String repName = "상품 없음";
            if (!items.isEmpty()) {
                repName = items.get(0).getProductName();
                if (items.size() > 1) {
                    repName += " 외 " + (items.size() - 1) + "건";
                }
            }
            
            Member member = adminMemberRepository.findById(order.getMemberSeq()).orElse(null);
            String mName = (member != null) ? member.getName() + " (" + member.getUsername() + ")" : "탈퇴회원";

            dtoList.add(AdminOrderSummaryDto.builder()
                    .order(order)
                    .representativeItemName(repName)
                    .memberName(mName)
                    .build());
        }

        return new PageImpl<>(dtoList, pageable, orderPage.getTotalElements());
    }
}
