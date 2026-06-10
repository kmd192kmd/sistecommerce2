package com.example.java.product.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            @RequestParam(name = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "searchType", defaultValue = "productName") String searchType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sortType", defaultValue = "latestDesc") String sortType,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        /*
            한 페이지에 보여줄 상품 요청 개수입니다.
        */
        int size = 10;

        if (page < 1) {
            page = 1;
        }

        int totalCount = productManageService.countProductRequests(
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
        List<ProductManageDto> products = productManageService.getProductRequests(
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

            이 값이 있어야 정렬 select에서 현재 선택한 값이 유지되고,
            페이지네이션을 눌러도 정렬 조건이 유지됩니다.
        */
        model.addAttribute("sortType", sortType);

        /*
            이 return 경로는 상품 관리 HTML 파일 위치에 맞춰야 합니다.

            현재 파일이 아래 위치라면:
            src/main/resources/templates/product/product-manage.html

            return "product/product-manage";

            만약 파일 위치가 다르면 이 문자열만 바꾸면 됩니다.
        */

        /*
            실제 상품 관리 HTML 파일 위치:
            src/main/resources/templates/product/manage.html

            Thymeleaf에서는 templates 폴더 아래 경로를 기준으로
            확장자 .html을 제외하고 작성합니다.

            따라서 product 폴더 안의 manage.html을 열려면
            return "product/manage"; 로 작성해야 합니다.
        */
        return "product/manage";
    }


    /*
        상품 관리 화면에서 선택한 상품을 삭제 처리합니다.

        현재 HTML의 삭제 버튼 JS가
        POST /products/manage/delete 로 요청을 보내고 있습니다.
    */
    @PostMapping("/products/manage/delete")
    public ResponseEntity<String> deleteProducts(
            @RequestParam(name = "productSeqs") List<Long> productSeqs) {

        productManageService.deleteProducts(productSeqs);

        return ResponseEntity.ok("삭제되었습니다.");
    }
}