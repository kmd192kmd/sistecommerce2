package com.example.java.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.java.member.entity.Member;
import com.example.java.admin.dto.AdminOrderSummaryDto;
import com.example.java.admin.repository.AdminMemberRepository;
import com.example.java.admin.repository.AdminOrderItemRepository;
import com.example.java.admin.repository.AdminOrdersRepository;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
    
    private final AdminMemberRepository adminMemberRepository;
    private final AdminOrdersRepository adminOrdersRepository;
    private final AdminOrderItemRepository adminOrderItemRepository;

    public Page<Member> getMembers(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return adminMemberRepository.findByUsernameContainingOrNameContainingOrNicknameContaining(
                    keyword, keyword, keyword, pageable);
        }
        return adminMemberRepository.findAll(pageable);
    }

    public Member getMember(Long seq) {
        return adminMemberRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateMemberStatus(Long seq, Integer status) {
        Member member = getMember(seq);
        member.changeStatus(status);
    }

    public List<AdminOrderSummaryDto> getMemberOrders(Long seq) {
        List<Orders> ordersList = adminOrdersRepository.findTop5ByMemberSeqOrderByOrderDateDesc(seq);
        List<AdminOrderSummaryDto> result = new ArrayList<>();
        
        for (Orders order : ordersList) {
            List<OrderItem> items = adminOrderItemRepository.findByOrderSeq(order.getSeq());
            String repName = "상품 없음";
            
            if (!items.isEmpty()) {
                repName = items.get(0).getProductName();
                if (items.size() > 1) {
                    repName += " 외 " + (items.size() - 1) + "건";
                }
            }
            
            result.add(AdminOrderSummaryDto.builder()
                    .order(order)
                    .representativeItemName(repName)
                    .build());
        }
        
        return result;
    }
}