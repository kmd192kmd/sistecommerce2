package com.example.java.product.util;

public class ChosungUtil {

    private static final char[] CHOSUNG = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    public static String getChosung(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int chosungIndex = (c - 0xAC00) / 28 / 21;
                sb.append(CHOSUNG[chosungIndex]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isChosungOnly(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            boolean isChosung = false;
            for (char ch : CHOSUNG) {
                if (c == ch) {
                    isChosung = true;
                    break;
                }
            }
            if (!isChosung) {
                return false;
            }
        }
        return true;
    }
}
