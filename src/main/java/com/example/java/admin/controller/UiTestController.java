package com.example.java.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiTestController {

	@GetMapping("/uiTest")
	public String Test404() {
		return "admin/404.html";
	}
	@GetMapping("/indexTest")
	public String TestIndex() {
		return "admin/index";
	}
	@GetMapping("/indexTest2")
	public String TestIndex2() {
		return "admin/alerts";
	}
	@GetMapping("/indexTest3")
	public String TestIndex3() {
		return "admin/avatars";
	}
	@GetMapping("/indexTest4")
	public String TestIndex4() {
		return "admin/badge";
	}
	@GetMapping("/indexTest5")
	public String TestIndex5() {
		return "admin/bar-chart";
	}
	@GetMapping("/indexTest6")
	public String TestIndex6() {
		return "admin/basic-tables";
	}
	@GetMapping("/indexTest7")
	public String TestIndex7() {
		return "admin/blank";
	}
	@GetMapping("/indexTest8")
	public String TestIndex8() {
		return "admin/buttons";
	}
	@GetMapping("/indexTest9")
	public String TestIndex9() {
		return "admin/calendar";
	}
}
