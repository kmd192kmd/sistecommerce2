package com.example.java.member.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * UserAgent 문자열을 분석해 접속 플랫폼을 반환.
 * 플랫폼 값: "ANDROID" | "IOS" | "PC"
 *
 * 판별 우선순위:
 *  1. Android 키워드
 *  2. iPhone / iPad 키워드 (iOS)
 *  3. 나머지 → PC
 */
public class PlatformDetector {

    private PlatformDetector() {}

    public static String detect(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "PC";

        String lower = ua.toLowerCase();

        if (lower.contains("android")) return "ANDROID";
        if (lower.contains("iphone") || lower.contains("ipad")) return "IOS";
        return "PC";
    }
}
