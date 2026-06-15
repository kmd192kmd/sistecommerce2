package com.example.java.admin.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.java.admin.dto.HotDealRequestDto;
import com.example.java.admin.hotdeal.Entity.HotDeal;
import com.example.java.admin.hotdeal.Entity.HotDealProduct;
import com.example.java.admin.service.HotDealAdminService;
import com.example.java.product.entity.Options;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class HotDealAdminController {

    private final HotDealAdminService hotDealAdminService;

    /**
     * 관리자: 신규 핫딜 생성 화면(HTML) 렌더링
     */
    @GetMapping("/hotdeal/create")
    public String createForm() {
        
        return "admin/hotdeal/create"; 
    }

    /**
     * 관리자: 폼(Form) 데이터 제출 처리
     */
    @PostMapping("/hotdeal/create")
    public String createHotDeal(@Valid @ModelAttribute HotDealRequestDto requestDto, RedirectAttributes rttr) {
    	 try {
             hotDealAdminService.createHotDeal(requestDto);
             return "redirect:/admin/hotdeal/list";
         } catch (IllegalArgumentException e) {
             // 재고 부족 등의 에러가 터지면 에러 메시지를 담아서 다시 생성 폼으로 쫓아냄
             rttr.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/admin/hotdeal/create";
         }

     }
    
    
    @GetMapping("/hotdeal/list")
    public String listForm(Model model) {
        List<HotDeal> hotdeals = hotDealAdminService.getAllHotDeals();
        model.addAttribute("hotdeals", hotdeals);
        return "admin/hotdeal/list";
    }
    
    
    @ResponseBody
    @GetMapping("/hot-deals/search-options")
    public ResponseEntity<List<Map<String, Object>>> searchOptions(@RequestParam("keyword") String keyword) {
        List<Map<String, Object>> result = hotDealAdminService.searchOptionsSafely(keyword);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/hotdeal/delete")
    public String deleteHotDeal(@RequestParam("seq") Long seq) {
        hotDealAdminService.deleteHotDeal(seq);
        return "redirect:/admin/hotdeal/list";
    }
    
    @GetMapping("/hotdeal/edit")
    public String editForm(@RequestParam("seq") Long seq, Model model) {
        HotDeal hotDeal = hotDealAdminService.getHotDeal(seq);
        if (hotDeal.getStatus() != 0) {
            return "redirect:/admin/hotdeal/list";
        }

        // 다건 조회로 여러 개를 리스트로 가져옵니다!
        List<HotDealProduct> products = hotDealAdminService.getHotDealProducts(seq);

        HotDealRequestDto dto = new HotDealRequestDto();
        dto.setSeq(hotDeal.getSeq());
        dto.setName(hotDeal.getName());
        dto.setStartDate(hotDeal.getStartDate());
        dto.setEndDate(hotDeal.getEndDate());
        dto.setDiscountRate(hotDeal.getDiscountRate());
        dto.setDiscountPrice(hotDeal.getDiscountPrice());

        // 프론트엔드로 여러 개의 상품 배열을 던져줍니다!
        model.addAttribute("hotDealProducts", products);
        model.addAttribute("hotDealDto", dto);
        return "admin/hotdeal/edit";
    }

    /**
     * 관리자: 핫딜 실제 수정 처리
     */
    @PostMapping("/hotdeal/edit")
    public String updateHotDeal(@Valid @ModelAttribute HotDealRequestDto requestDto, RedirectAttributes rttr) {
    	 try {
             hotDealAdminService.updateHotDeal(requestDto);
             return "redirect:/admin/hotdeal/list";
         } catch (Exception e) { // IllegalArgumentException, IllegalStateException 모두 잡음
             // 에러가 터지면 다시 수정 폼으로 쫓아냄 (seq 유지)
             rttr.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/admin/hotdeal/edit?seq=" + requestDto.getSeq();
         }
    }
    
}