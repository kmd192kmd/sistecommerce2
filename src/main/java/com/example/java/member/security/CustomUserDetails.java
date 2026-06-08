package com.example.java.member.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.java.member.entity.Member;

import lombok.Getter;

/**
 * Spring Security 기본 {@code User}에는 username/password/권한만 담겨 회원 PK(seq)를 보관할 수 없다.
 * 로그인 이후 컨트롤러에서 {@code @AuthenticationPrincipal}로 회원 seq를 바로 꺼내 쓰기 위한 커스텀 구현체.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long memberSeq;
    private final String username;
    private final String password;
    private final String role;

    public CustomUserDetails(Member member) {
        this.memberSeq = member.getSeq();
        this.username = member.getUsername();
        this.password = member.getPassword();
        this.role = member.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기존 User.builder().authorities(role) 와 동일하게 role 문자열 하나를 권한으로 사용.
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
