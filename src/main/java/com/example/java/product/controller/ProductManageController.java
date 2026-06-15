package com.example.java.product.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.product.dto.ProductManageDto;
import com.example.java.product.service.ProductManageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductManageController {

    private final ProductManageService productManageService;


    /*
        상품 관리 페이지를 조회합니다.

        화면 주소:
        GET /products/manage

        기능:
        - 승인상태 필터
        - 기간 검색
        - 상품명 / 판매처명 / 상품번호 검색
        - 페이지네이션
    */
    @GetMapping("/products/manage")
    public String productManage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "searchType", defaultValue = "productName") String searchType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sortType", defaultValue = "latestDesc") String sortType,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        /*
            판매처 계정만 상품 관리 페이지에 접근할 수 있습니다.

            현재 CustomUserDetails의 memberSeq 필드는
            판매처 로그인일 때 seller.seq 값을 담습니다.
        */
        if (userDetails == null || !"ROLE_SELLER".equals(userDetails.getRole())) {
            return "redirect:/";
        }

        Long sellerSeq = userDetails.getMemberSeq();

        /*
            한 페이지에 보여줄 상품 요청 개수입니다.
        */
        int size = 10;

        if (page < 1) {
            page = 1;
        }

        int totalCount = productManageService.countSellerProductRequests(
                sellerSeq,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );

        int totalPage = (int) Math.ceil((double) totalCount / size);

        if (totalPage < 1) {
            totalPage = 1;
        }

        if (page > totalPage) {
            page = totalPage;
        }

        /*
            상품 목록 정렬 조건 sortType을 Service로 전달합니다.

            sortType:
            - salesDesc  : 판매량순
            - latestDesc : 최신순
            - priceAsc   : 가격순
            - ratingDesc : 별점순
        */
        List<ProductManageDto> products = productManageService.getSellerProductRequests(
                sellerSeq,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword,
                sortType,
                page,
                size
        );

        /*
            페이지네이션 하단에 표시할 시작 페이지와 끝 페이지를 계산합니다.

            예:
            현재 page = 7
            startPage = 1
            endPage = 10
        */
        int pageBlockSize = 10;
        int startPage = ((page - 1) / pageBlockSize) * pageBlockSize + 1;
        int endPage = Math.min(startPage + pageBlockSize - 1, totalPage);

        model.addAttribute("products", products);
        model.addAttribute("totalCount", totalCount);

        model.addAttribute("page", page);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        model.addAttribute("approvalStatus", approvalStatus);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);

        /*
            현재 선택한 정렬 조건을 화면으로 다시 내려줍니다.
        */
        model.addAttribute("sortType", sortType);

        return "product/manage";
    }


    /*
        상품 관리 화면에서 선택한 상품을 삭제 처리합니다.

        현재 HTML의 삭제 버튼 JS가
        POST /products/manage/delete 로 요청을 보내고 있습니다.
    */
    @PostMapping("/products/manage/delete")
    public ResponseEntity<String> deleteProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "productSeqs") List<Long> productSeqs) {

        if (userDetails == null || !"ROLE_SELLER".equals(userDetails.getRole())) {
            return ResponseEntity.status(403).body("판매처만 삭제할 수 있습니다.");
        }

        Long sellerSeq = userDetails.getMemberSeq();

        productManageService.deleteSellerProducts(sellerSeq, productSeqs);

        return ResponseEntity.ok("삭제되었습니다.");
    }
}