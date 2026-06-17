package com.example.java.groupbuy.payment;

/**
 * 공구 → 결제 경계로 넘기는 결제 요청. 모든 가격은 '구매 시점 스냅샷'이다.
 * 결제팀 구현체는 이 값으로 orders + order_item + payment 를 한 트랜잭션에 생성한다.
 *
 * <p>order_item 매핑: optionsSeq→options_seq, originalPrice→original_price,
 * finalPrice→final_price, participationDiscount→participation_discount,
 * participationSeq→participation_seq.
 * product_name(상품명 스냅샷)은 결제팀이 options→product로 직접 채운다(공구가 product를 로딩하지 않도록 경계에서 제외).
 * coupon_discount/hotdeal_discount=0, quantity=1, sub_total_price=finalPrice 는
 * 공구 불변값(1인1상품·수량1·쿠폰/핫딜 없음)이라 결제팀이 채운다.
 *
 * @param memberSeq            결제 회원 seq
 * @param participationSeq     공구 참여 seq (order_item.participation_seq 연결 = 공구 주문 식별)
 * @param optionsSeq           상품 옵션 seq (order_item.options_seq)
 * @param originalPrice        정가 1개 (옵션 추가금 포함)
 * @param finalPrice           실제 결제가 1개 = 공구 할인가 + 옵션 추가금
 * @param participationDiscount 공구 할인액 = originalPrice - finalPrice
 */
public record GroupBuyPaymentCommand(
        Long memberSeq,
        Long participationSeq,
        Long optionsSeq,
        int originalPrice,
        int finalPrice,
        int participationDiscount
) {
}
