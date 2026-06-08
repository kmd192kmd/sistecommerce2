package com.example.java.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(
            name = "order_item_seq_generator",
            sequenceName = "order_item_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq_generator")
    private Long seq;

    /**
     * 변경 후 추가된 컬럼.
     *
     * order_item.order_seq -> orders.seq
     *
     * 한 주문에 여러 주문상품이 들어갈 수 있으므로,
     * order_item 쪽에서 orders를 참조한다.
     */
    @Column(name = "order_seq", nullable = false)
    private Long orderSeq;

    @Column(name = "participation_seq")
    private Long participationSeq;

    @Column(name = "options_seq", nullable = false)
    private Long optionsSeq;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * 구매 당시 상품 1개 원가.
     */
    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    @Column(name = "hotdeal_discount", nullable = false)
    @Builder.Default
    private Integer hotdealDiscount = 0;

    /**
     * 해당 주문상품 라인에 배분된 쿠폰 할인금액.
     */
    @Column(name = "coupon_discount", nullable = false)
    @Builder.Default
    private Integer couponDiscount = 0;

    @Column(name = "participation_discount", nullable = false)
    @Builder.Default
    private Integer participationDiscount = 0;

    /**
     * 할인 적용 후 상품 1개 가격.
     */
    @Column(name = "final_price", nullable = false)
    private Integer finalPrice;

    /**
     * 해당 상품 라인의 최종 합계.
     * 예: final_price * quantity
     */
    @Column(name = "sub_total_price", nullable = false)
    private Integer subTotalPrice;

    @Column(name = "refund_quantity", nullable = false)
    @Builder.Default
    private Integer refundQuantity = 0;

    @Column(name = "refund_price", nullable = false)
    @Builder.Default
    private Integer refundPrice = 0;

    /**
     * DDL 주석 기준:
     * 0: 주문완료
     * 1: 상품준비중
     * 2: 배송중
     * 3: 배송완료
     * 4: 부분환불
     * 5: 전체환불
     * 6: 주문취소
     * 7: 반품요청
     * 8: 반품진행중
     * 9: 반품완료
     */
    @Column(name = "item_status", nullable = false)
    @Builder.Default
    private Integer itemStatus = 0;

    @Column(name = "return_quantity")
    private Integer returnQuantity;
}