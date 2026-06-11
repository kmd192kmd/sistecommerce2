package com.example.java.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.admin.dto.GroupBuyCreateDto;
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

        return "redirect:/";
    }

}
