package com.example.java.mypage.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final JdbcTemplate jdbcTemplate;
    private final MemberRepository memberRepository;

    @GetMapping({"", "/"})
    public String mypageRoot() {
        return "redirect:/mypage/orders";
    }

    /**
     * 마이페이지 - 주문목록 및 배송조회
     */
    @GetMapping("/orders")
    public String getOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            Principal principal,
            Model model) {

        // 로그인 확인
        if (principal == null) {
            return "redirect:/member/login";
        }

        // 로그인한 회원의 정보를 조회
        Member member = memberRepository.findByUsername(principal.getName()).orElse(null);
        if (member == null) {
            return "redirect:/member/login";
        }
        
        Long memberSeq = member.getSeq();

        // DB에서 주문, 상품, 배송 정보를 조인하여 가져옵니다.
        String sql = """
            SELECT 
                TO_CHAR(o.order_date, 'YYYY.MM.DD HH24:MI') as orderDate,
                COALESCE(d.status, '배송준비중') as deliveryStatus,
                (
                    SELECT pi.image_url
                    FROM product_image pi
                    WHERE pi.product_seq = p.seq
                      AND pi.thumbnail_yn = 'Y'
                      AND pi.status = 'NORMAL'
                    ORDER BY pi.image_order ASC
                    FETCH FIRST 1 ROWS ONLY
                ) as image,
                oi.product_name as name,
                oi.final_price as price,
                oi.quantity as qty,
                COALESCE(d.tracking_number, '발급대기') as trackingNumber
            FROM orders o
            JOIN order_item oi ON o.order_item_seq = oi.seq
            JOIN options opt ON oi.options_seq = opt.seq
            JOIN product p ON opt.product_seq = p.seq
            LEFT JOIN delivery d ON o.seq = d.orders_seq
            WHERE o.member_seq = ?
            """;

        // 검색어가 있으면 필터링 조건 추가
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += " AND p.product_name LIKE '%" + keyword.trim() + "%' ";
        }
        
        sql += " ORDER BY o.seq DESC";

        try {
            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql, memberSeq);
            
            // 이미지 기본값 처리
            for (Map<String, Object> order : orders) {
                if (order.get("image") == null || order.get("image").toString().trim().isEmpty()) {
                    order.put("image", "/images/default-product.png"); // 기본 썸네일 경로
                }
            }
            
            model.addAttribute("orders", orders);
            model.addAttribute("keyword", keyword);
            
        } catch (Exception e) {
            log.error("주문 목록 조회 중 에러 발생: ", e);
        }

        return "mypage/orders";
    }
}
