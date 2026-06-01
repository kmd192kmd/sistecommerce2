package com.example.java;

import com.example.java.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        MemberDto auth = (MemberDto) session.getAttribute("auth");
        model.addAttribute("loginMember", auth);
        return "index";
    }
}