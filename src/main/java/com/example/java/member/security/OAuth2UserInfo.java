package com.example.java.member.security;

/**
 * provider별 OAuth2 사용자 정보 파싱 인터페이스.
 * Google/Naver 각각 응답 구조가 달라 구현체에서 처리.
 */
public interface OAuth2UserInfo {

    /** provider 식별자 (예: "GOOGLE", "NAVER") */
    String getProvider();

    /** provider 내부 고유 ID (Google: sub, Naver: id) */
    String getProviderId();

    String getName();

    String getEmail();

    /** username 생성: "{provider소문자}_{providerId}" */
    default String toUsername() {
        return getProvider().toLowerCase() + "_" + getProviderId();
    }
}
