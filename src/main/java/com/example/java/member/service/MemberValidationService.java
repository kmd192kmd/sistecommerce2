package com.example.java.member.service;

import org.springframework.stereotype.Service;

import com.example.java.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberValidationService {
	
	private final MemberRepository memberRepository;
	
	public boolean existsByUsername(String username) {

		return memberRepository.existsByUsername(username);
	}

	public boolean existsByNickname(String nickname) {

		return memberRepository.existsByNickname(nickname);
	}

	public boolean existsByEmail(String email) {

		return memberRepository.existsByEmail(email);
	}

}
