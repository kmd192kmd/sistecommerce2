package com.example.java.member.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.member.service.MemberValidationService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberApiController {
	
	private final MemberValidationService memberValidationService;
	
	@GetMapping("/check-username")
	public Map<String, Boolean> checkUsername(@RequestParam(name = "username") String username) {
		
		boolean exists = memberValidationService.existsByUsername(username);
		return Map.of("available", !exists);
	}
	
	@GetMapping("/check-nickname")
	public Map<String, Boolean> checkNickname(@RequestParam(name = "nickname") String nickname) {
		
		boolean exists = memberValidationService.existsByNickname(nickname);
		return Map.of("available", !exists);
	}
	

}
