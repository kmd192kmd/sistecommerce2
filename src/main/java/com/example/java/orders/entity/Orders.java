package com.example.java.orders.entity;

import java.time.LocalDateTime;

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
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(
            name = "orders_seq_generator",
            sequenceName = "orders_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq_generator")
    private Long seq;

    @Column(name = "member_seq", nullable = false)
    private Long memberSeq;

    /*
        변경 전:
        orders.order_item_seq 컬럼이 있었음.

        변경 후:
        orders는 더 이상 order_item을 직접 참조하지 않음.
        order_item.order_seq가 orders.seq를 참조함.
     */

    @Column(name = "member_coupon_seq")
    private Long memberCouponSeq;

    @Column(name = "order_uid", nullable = false, unique = true, length = 100)
    private String orderUid;

    @Column(name = "product_total_price", nullable = false)
    private Integer productTotalPrice;

    @Column(name = "coupon_discount", nullable = false)
    @Builder.Default
    private Integer couponDiscount = 0;

    @Column(name = "hotdeal_discount", nullable = false)
    @Builder.Default
    private Integer hotdealDiscount = 0;

    @Column(name = "field", length = 255)
    private String field;

    @Column(name = "final_price", nullable = false)
    private Integer finalPrice;

    @Column(name = "total_refund_price", nullable = false)
    @Builder.Default
    private Integer totalRefundPrice = 0;

    @Column(name = "remain_price", nullable = false)
    private Integer remainPrice;

    /**
     * DDL 주석 기준:
     * 0: 주문생성
     * 1: 결제대기
     * 2: 결제완료
     * 3: 상품준비중
     * 4: 부분배송중
     * 5: 배송중
     * 6: 배송완료
     * 7: 부분환불
     * 8: 전체환불
     * 9: 주문취소
     */
    @Column(name = "order_status", nullable = false)
    @Builder.Default
    private Integer orderStatus = 0;

    /**
     * DDL 주석 기준:
     * 0: 결제대기
     * 1: 가상계좌입금대기
     * 2: 결제완료
     * 3: 결제실패
     * 4: 부분환불
     * 5: 전체환불
     * 6: 결제취소
     */
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private Integer paymentStatus = 0;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "regdate", nullable = false)
    private LocalDateTime regdate;

    @Column(name = "zipcode", nullable = false, length = 5)
    private String zipcode;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "address_detail", length = 100)
    private String addressDetail;

    @Column(name = "curr_latitude", nullable = false)
    private Double currLatitude;

    @Column(name = "curr_longitude", nullable = false)
    private Double currLongitude;
}