package com.example.java.member.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberSecurityService implements UserDetailsService {

	private final MemberRepository memberRepository;

	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Member member = memberRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다: " + username));
		// Spring Security가 제공하는 기본 User는 seq를 못 담으므로, 
		// seq를 보관한 CustomUserDetails를 반환한다.
		// (GroupBuyApiController에서 @AuthenticationPrincipal 로 회원 seq를 꺼내 쓰기 위함)
		return new CustomUserDetails(member);
	}

}
