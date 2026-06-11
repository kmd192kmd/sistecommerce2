package com.example.java.admin.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.java.admin.dto.GroupBuyAdminListDto;
import com.example.java.admin.dto.GroupBuyCreateDto;
import com.example.java.admin.dto.GroupBuySearchDto;
import com.example.java.admin.service.GroupBuyAdminService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class GroupBuyAdminController {
	
	private final GroupBuyAdminService groupBuyAdminService;
	
	@GetMapping("/group-buys/create")
	public String createForm(@RequestParam(value = "optionsSeqs") List<Long> optionsSeqs, Model model) {

	    GroupBuyCreateDto dto =
	    		groupBuyAdminService.getCreateForm(optionsSeqs);

	    model.addAttribute("dto", dto);

	    return "admin/group-buy/add";
	}
	
	@PostMapping("/group-buys")
    public String create(
            @ModelAttribute GroupBuyCreateDto dto) {

        Long groupBuySeq =
                groupBuyAdminService.create(dto);
        
        System.out.println(groupBuySeq);

        return "redirect:/admin/group-buys";
    }

    // --- 공동구매 관리 목록 페이지 렌더링 ---
    @GetMapping("/group-buys")
    public String list(@ModelAttribute("search") GroupBuySearchDto searchDto,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        Page<GroupBuyAdminListDto> page = groupBuyAdminService.searchGroupBuys(searchDto, pageable);
        
        model.addAttribute("list", page.getContent());
        model.addAttribute("hasNext", page.hasNext());
        model.addAttribute("totalElements", page.getTotalElements());
        
        return "admin/group-buy/list";
    }

    // --- 더보기 비동기 호출을 위한 REST API ---
    @GetMapping("/api/group-buys")
    @ResponseBody
    public ResponseEntity<Page<GroupBuyAdminListDto>> apiList(
            @ModelAttribute GroupBuySearchDto searchDto,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<GroupBuyAdminListDto> page = groupBuyAdminService.searchGroupBuys(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- 선택 목록 강제 중지 기능 ---
    @PostMapping("/group-buys/batch-stop")
    public String batchStop(@RequestParam(value = "seqs", required = false) List<Long> seqs) {
        groupBuyAdminService.batchStop(seqs);
        return "redirect:/admin/group-buys";
    }
}
