package com.example.java.member.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.example.java.member.entity.Member;

import lombok.Getter;

/**
 * 일반 로그인(UserDetails)과 Google OIDC 로그인(OidcUser) 모두 지원.
 * 컨트롤러에서 {@code @AuthenticationPrincipal CustomUserDetails}로 통일해서 사용 가능.
 */
@Getter
public class CustomUserDetails implements UserDetails, OidcUser {
    private static final long serialVersionUID = 1L;

    private final Long memberSeq;
    private final String username;
    private final String password;
    private final String role;
    private final Integer status;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    /** 일반 로그인용 */
    public CustomUserDetails(Member member) {
        this.memberSeq  = member.getSeq();
        this.username   = member.getUsername();
        this.password   = member.getPassword();
        this.role       = member.getRole();
        this.status     = member.getStatus();
        this.attributes = Collections.emptyMap();
        this.idToken    = null;
        this.userInfo   = null;
    }

    /** Google OIDC 로그인용 */
    public CustomUserDetails(Member member, Map<String, Object> attributes,
                              OidcIdToken idToken, OidcUserInfo userInfo) {
        this.memberSeq  = member.getSeq();
        this.username   = member.getUsername();
        this.password   = member.getPassword();
        this.role       = member.getRole();
        this.status     = member.getStatus();
        this.attributes = attributes;
        this.idToken    = idToken;
        this.userInfo   = userInfo;
    }

    /** OidcUser */
    @Override public Map<String, Object> getClaims() { return idToken != null ? idToken.getClaims() : Collections.emptyMap(); }
    @Override public OidcUserInfo getUserInfo() { return userInfo; }
    @Override public OidcIdToken getIdToken() { return idToken; }

    /** OAuth2User */
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    /** OAuth2User — principal name (username으로 통일) */
    @Override
    public String getName() { return username; }

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

    /**
     * 탈퇴완료(status=5) 계정은 로그인 차단(DisabledException).
     * 탈퇴보류중(4)은 복구를 위해 로그인 허용. 휴면(2)/정지(3)는 추후 별도 처리.
     */
    @Override
    public boolean isEnabled() {
        return status == null
                || status != com.example.java.member.constant.MemberStatus.WITHDRAWN;
    }
}
