package com.example.java;

import java.util.List;
import java.util.stream.Collectors;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.ProductListService;
import com.example.java.storefront.SampleProducts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.java.member.security.CustomUserDetails;
import com.example.java.retail.service.RetailRecommendationService;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ProductListService productListService;
    private final RetailRecommendationService retailRecommendationService;

    // 메인 화면 (COM-MAIN-01). auth 는 GlobalModelAdvice 가 주입한다.
    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails auth, Model model) {
        model.addAttribute("groupBuys", SampleProducts.groupBuys());
        model.addAttribute("hotdeals", SampleProducts.hotdeals());
        
        // 실시간 인기 베스트 상품: 캐시된 추천 상품 목록 가져오기 (상위 4개)
        List<ProductDto> populars = productListService.getPopularProducts().stream()
                .limit(4)
                .collect(Collectors.toList());
        model.addAttribute("populars", populars);
        
        // 맞춤형 AI 추천 상품 가져오기
        Long memberSeq = (auth != null) ? auth.getMemberSeq() : null;
        List<ProductDto> recommendations = retailRecommendationService.recommendProducts(memberSeq);
        model.addAttribute("recommendations", recommendations);
        
        return "index";
    }
}
