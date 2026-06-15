package com.example.java.member.util;

/**
 * 개인정보 비가역 마스킹 유틸.
 *
 * - 부분 마스킹(maskName/maskPhone/maskEmail/maskMiddle): 원본 복구 불가능한 표시용 마스킹.
 * - anonymized(): UNIQUE NOT NULL 컬럼(username/nickname 등)에 쓸 seq 기반 익명 식별자.
 *   탈퇴회원 다수를 마스킹해도 유니크 제약을 위반하지 않도록 seq 를 포함한다.
 *
 * 주의: 여기서 만든 값은 되돌릴 수 없어야 한다(전자상거래법상 '파기' 요건).
 */
public final class MaskingUtil {

    /** 고정 마스킹 토큰 (유니크 제약이 없는 보관 테이블 등에 사용) */
    public static final String MASK = "***";

    private MaskingUtil() {}

    /** 이름: 홍길동 → 홍*동, 김철 → 김*, 한 글자 → * */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        int len = name.length();
        if (len == 1) {
            return "*";
        }
        if (len == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(len - 2) + name.charAt(len - 1);
    }

    /** 전화: 앞 3자리, 뒤 4자리만 남기고 마스킹 (구분자 포함 길이 유지) */
    public static String maskPhone(String phone) {
        return maskMiddle(phone, 3, 4);
    }

    /** 이메일: 로컬파트 앞 2자만 남기고 마스킹, 도메인 유지 (abcde@x.com → ab***@x.com) */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return maskMiddle(email, 2, 0);
        }
        String local  = email.substring(0, at);
        String domain = email.substring(at); // '@' 포함
        String maskedLocal = (local.length() <= 2)
                ? "*".repeat(local.length())
                : local.substring(0, 2) + "*".repeat(local.length() - 2);
        return maskedLocal + domain;
    }

    /**
     * 일반 가운데 마스킹: 앞 keepFront, 뒤 keepBack 자리만 남기고 '*' 처리.
     * 남길 길이보다 짧으면 전부 마스킹.
     */
    public static String maskMiddle(String value, int keepFront, int keepBack) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int len = value.length();
        if (len <= keepFront + keepBack) {
            return "*".repeat(len);
        }
        return value.substring(0, keepFront)
                + "*".repeat(len - keepFront - keepBack)
                + value.substring(len - keepBack);
    }

    /**
     * UNIQUE NOT NULL 컬럼용 익명 식별자.
     * 예) anonymized("WD", 123) → "WD_123"
     */
    public static String anonymized(String prefix, Long seq) {
        return prefix + "_" + seq;
    }
}
