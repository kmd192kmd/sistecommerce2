package com.example.java.productrequest.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.ProductManageDto;
import com.example.java.product.service.ProductManageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class ProductRequestAdminController {

    private final ProductManageService productManageService;

    @GetMapping("/product-requests")
    public String productRequestManage(
            @RequestParam(name = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "searchType", defaultValue = "productName") String searchType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sortType", defaultValue = "latestDesc") String sortType,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

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

        List<ProductManageDto> products =
                productManageService.getProductRequests(
                        approvalStatus,
                        startDate,
                        endDate,
                        searchType,
                        keyword,
                        sortType,
                        page,
                        size
                );

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
        model.addAttribute("sortType", sortType);

        return "admin/product-request/list";
    }
}