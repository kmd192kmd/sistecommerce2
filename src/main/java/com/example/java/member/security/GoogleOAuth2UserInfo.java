package com.example.java.member.security;

import java.util.Map;

/**
 * Google OIDC 응답 파싱.
 * attributes 키: sub, name, email
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() { return "GOOGLE"; }

    @Override
    public String getProviderId() { return (String) attributes.get("sub"); }

    @Override
    public String getName() { return (String) attributes.get("name"); }

    @Override
    public String getEmail() { return (String) attributes.get("email"); }
}
