package com.example.java.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiTestController {

	@GetMapping("/uiTest")
	public String Test404() {
		return "redirect:404.html";
	}
	@GetMapping("/indexTest")
	public String TestIndex() {
		return "forward:index.html";
	}
	@GetMapping("/indexTest2")
	public String TestIndex2() {
		return "index.html";
	}
}
