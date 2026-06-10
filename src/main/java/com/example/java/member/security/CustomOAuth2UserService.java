package com.example.java.member.security;

import java.util.Map;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, attributes);

        String username = userInfo.toUsername(); // "google_109374..."

        Member member = memberRepository.findByUsername(username)
                .map(existing -> {
                    existing.updateSocialInfo(userInfo.getName(), userInfo.getEmail());
                    return existing;
                })
                .orElseGet(() -> {
                    String nickname = resolveNickname(userInfo.getName());
                    return memberRepository.save(
                            Member.ofSocial(username, nickname, userInfo.getName(),
                                    userInfo.getEmail(), userInfo.getProvider())
                    );
                });

        log.debug("구글 OIDC 로그인: {}", username);

        return new CustomUserDetails(member, attributes, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 provider: " + registrationId);
        };
    }

    /**
     * 닉네임 중복 시 "_2", "_3" suffix 부여
     */
    private String resolveNickname(String baseName) {
        String nickname = baseName;
        int suffix = 2;
        while (memberRepository.existsByNickname(nickname)) {
            nickname = baseName + "_" + suffix++;
        }
        return nickname;
    }
}
