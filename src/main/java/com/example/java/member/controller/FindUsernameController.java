package com.example.java.member.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.member.dto.FindUsernameRequestDto;
import com.example.java.member.dto.FindUsernameResultDto;
import com.example.java.member.service.FindUsernameService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/member/find-username")
@RequiredArgsConstructor
public class FindUsernameController {

    private final FindUsernameService findUsernameService;

    @GetMapping
    public String findUsernamePage() {
        return "member/find-username";
    }

    @PostMapping
    public String findUsername(
            @ModelAttribute FindUsernameRequestDto dto,
            Model model) {

        List<FindUsernameResultDto> results = findUsernameService.findByNameAndEmail(dto.getName(), dto.getEmail());

        model.addAttribute("results", results);
        model.addAttribute("searched", true);
        return "member/find-username";
    }
}
