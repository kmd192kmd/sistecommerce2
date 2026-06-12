package com.example.java.admin.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.admin.dto.AdminProductListDto;
import com.example.java.admin.dto.AdminProductSearchDto;
import com.example.java.admin.service.AdminProductService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public String list(@ModelAttribute("search") AdminProductSearchDto search, Model model) {
        
        Slice<AdminProductListDto> slice = adminProductService.getProductList(search, PageRequest.of(0, 10));
        
        model.addAttribute("list", slice.getContent());
        model.addAttribute("hasNext", slice.hasNext());
        
        return "admin/product/list";
    }
}
