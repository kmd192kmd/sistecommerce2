package com.example.java.storefront;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.java.member.dto.MemberDto;
import com.example.java.product.dto.CategoryDto;
import com.example.java.product.service.CategoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 모든 화면(헤더 fragment)에서 쓰는 공통 모델 속성 주입.
 * - auth: 세션의 로그인 사용자(없으면 null) → 헤더 로그인/로그아웃 분기에 사용
 * - globalCategories: 카테고리 트리 목록 정보 제공
 * - recentKeywords: 세션에 저장된 최근 검색어 목록 제공
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CategoryService categoryService;


    // 모든 페이지에 카테고리 트리 정보 주입
    @ModelAttribute("globalCategories")
    public List<CategoryDto> globalCategories() {
        return categoryService.getCategoryTree();
    }
    
    // 세션의 최근 검색어 노출
    @SuppressWarnings("unchecked")
    @ModelAttribute("recentKeywords")
    public List<String> recentKeywords(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return List.of();
        }
        List<String> list = (List<String>) session.getAttribute("recentKeywords");
        return list != null ? list : List.of();
    }
}
