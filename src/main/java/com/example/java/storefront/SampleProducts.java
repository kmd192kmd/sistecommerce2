package com.example.java.storefront;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 스토어프론트 템플릿 확인용 더미 데이터.
 * 실제 서비스/리포지토리 연동 전까지 화면을 렌더링해 보기 위한 임시 데이터다.
 * (DB 연동 시 이 클래스는 제거하고 Service 결과로 교체할 것)
 */
public final class SampleProducts {

    private SampleProducts() {}

    private static final String IMG1 = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=400&auto=format&fit=crop";
    private static final String IMG2 = "https://images.unsplash.com/photo-1608248597481-496100c80836?q=80&w=400&auto=format&fit=crop";
    private static final String IMG3 = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?q=80&w=400&auto=format&fit=crop";
    private static final String IMG4 = "https://images.unsplash.com/photo-1595541913571-36573b7a696f?q=80&w=400&auto=format&fit=crop";

    /** 일반 상품 카드 한 장 */
    private static Map<String, Object> card(long seq, String name, String image,
                                            int price, int originalPrice, int discountRate,
                                            String rating, int reviewCount, String badge) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", seq);
        m.put("name", name);
        m.put("image", image);
        m.put("price", price);
        m.put("originalPrice", originalPrice);
        m.put("discountRate", discountRate);
        m.put("rating", rating);
        m.put("reviewCount", reviewCount);
        m.put("badge", badge);
        return m;
    }

    /** 핫딜 상품 (메인 골드박스) */
    public static List<Map<String, Object>> hotdeals() {
        return List.of(
            card(101, "[골드프레시] 엄선 한돈 대패 삼겹살 1kg (냉동)", IMG1, 14900, 21900, 32, "★★★★★", 4215, "골드프레시"),
            card(102, "최고급 호텔식 타올 수건 40수 고중량 10장 세트", IMG2, 19800, 32000, 38, "★★★★★", 890, "골드배송"),
            card(103, "이중 미세모 파스텔 인체공학 칫솔 12개입 세트", IMG4, 9900, 15900, 37, "★★★★☆", 1510, "골드배송"),
            card(104, "파워 하이토크 진공 블렌더 믹서기 대용량 1500W", IMG3, 79000, 119000, 33, "★★★★☆", 240, "골드배송")
        );
    }

    /** 인기 상품 (실시간 베스트) */
    public static List<Map<String, Object>> populars() {
        return List.of(
            card(201, "스마트 패드 프로 에디션 11인치 Wi-Fi 128GB", IMG3, 549000, 0, 0, "★★★★★", 3104, "골드배송"),
            card(202, "액티브 노이즈캔슬링 무선 하이파이 헤드폰", IMG4, 189000, 0, 0, "★★★★☆", 784, "골드배송"),
            card(203, "[골드프레시] 통밀 벨기에 버터 와플 믹스 600g", IMG1, 8900, 0, 0, "★★★★★", 1412, "골드프레시"),
            card(204, "VLOG 입문용 가성비 미러리스 디카 세트", IMG2, 499000, 0, 0, "★★★★☆", 182, "골드배송")
        );
    }

    /** 쇼핑몰 목록 (12개) */
    public static List<Map<String, Object>> products() {
        return List.of(
            populars().get(0), populars().get(1), populars().get(2), populars().get(3),
            hotdeals().get(0), hotdeals().get(1), hotdeals().get(2), hotdeals().get(3),
            card(301, "북유럽 빈티지 원목 다용도 인테리어 스툴", IMG2, 38000, 0, 0, "★★★★★", 195, "골드배송"),
            card(302, "미니멀 가죽 미드나잇 아날로그 손목시계", IMG3, 45000, 0, 0, "★★★★☆", 84, "골드배송"),
            card(303, "유기농 순면 규조토 항균 욕실 발매트", IMG1, 14200, 0, 0, "★★★★★", 233, "골드배송"),
            card(304, "올데이 테크 통기성 메쉬 스니커즈", IMG4, 52000, 0, 0, "★★★★★", 612, "골드배송")
        );
    }

    /** 상품 상세 1건 */
    public static Map<String, Object> product(long seq) {
        Map<String, Object> m = new LinkedHashMap<>(products().get(0));
        m.put("seq", seq);
        m.put("options", List.of("기본형 / 1kg", "대용량 / 2kg", "선물세트"));
        m.put("description", "프리미엄 엄선 상품입니다. 산지직송으로 신선하게 배송됩니다.");
        return m;
    }

    /** 공동구매 카드 한 장 */
    private static Map<String, Object> groupBuyCard(long seq, String name, String image,
                                                    int finalPrice, int originalPrice, int discountRate,
                                                    int minCount, int currentCount, String remainText) {
        int progress = Math.min(100, (int) Math.round(currentCount * 100.0 / Math.max(1, minCount)));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", seq);
        m.put("name", name);
        m.put("image", image);
        m.put("finalPrice", finalPrice);
        m.put("originalPrice", originalPrice);
        m.put("discountRate", discountRate);
        m.put("minCount", minCount);
        m.put("currentCount", currentCount);
        m.put("progress", progress);
        m.put("remainText", remainText);
        return m;
    }

    /** 공동구매 목록 */
    public static List<Map<String, Object>> groupBuys() {
        return List.of(
            groupBuyCard(401, "[공구] 프리미엄 유기농 밀키트 30종 패키지", IMG1, 25900, 39800, 35, 100, 78, "2일 14:30:00"),
            groupBuyCard(402, "[공구] 무선 노이즈캔슬링 이어버드 PRO", IMG4, 89000, 149000, 40, 50, 50, "마감임박 03:14:55"),
            groupBuyCard(403, "[공구] 호텔식 구스다운 차렵이불 세트", IMG2, 119000, 199000, 40, 80, 32, "5일 09:00:00"),
            groupBuyCard(404, "[공구] 대용량 스테인리스 보온병 1.5L", IMG3, 18900, 32000, 41, 60, 41, "1일 22:10:00")
        );
    }

    /** 공동구매 상세 1건 */
    public static Map<String, Object> groupBuy(long seq) {
        Map<String, Object> m = new LinkedHashMap<>(groupBuys().get(0));
        m.put("seq", seq);
        m.put("description", "정해진 기간 안에 최소 인원이 모이면 고정 할인가로 구매되는 공동구매 상품입니다.");
        return m;
    }

    /** 주문 목록 (마이페이지 주문내역) */
    public static List<Map<String, Object>> orders() {
        return List.of(
            order("2026-05-30", "배송완료 (05.31 도착)", "[골드프레시] 엄선 한돈 대패 삼겹살 1kg", IMG1, 14900, 2),
            order("2026-05-22", "배송중", "최고급 호텔식 타올 수건 40수 10장 세트", IMG2, 19800, 1),
            order("2026-05-10", "배송준비중", "파워 하이토크 진공 블렌더 믹서기 1500W", IMG3, 79000, 1)
        );
    }

    private static Map<String, Object> order(String orderDate, String status, String name,
                                             String image, int price, int qty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderDate", orderDate);
        m.put("status", status);
        m.put("name", name);
        m.put("image", image);
        m.put("price", price);
        m.put("qty", qty);
        return m;
    }

    /** 장바구니 항목 */
    public static List<Map<String, Object>> cartItems() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("seq", 1L); a.put("name", "[골드프레시] 엄선 한돈 대패 삼겹살 1kg"); a.put("image", IMG1);
        a.put("option", "대용량 / 2kg"); a.put("price", 14900); a.put("qty", 2);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("seq", 2L); b.put("name", "최고급 호텔식 타올 수건 40수 10장 세트"); b.put("image", IMG2);
        b.put("option", "그레이"); b.put("price", 19800); b.put("qty", 1);
        return List.of(a, b);
    }
}
