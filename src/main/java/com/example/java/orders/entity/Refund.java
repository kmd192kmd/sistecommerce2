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

import java.time.LocalDateTime;

@Entity
@Table(name = "refund")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(
            name = "refund_seq_generator",
            sequenceName = "refund_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refund_seq_generator")
    private Long seq;

    @Column(name = "order_item_seq", nullable = false)
    private Long orderItemSeq;

    @Column(name = "payment_seq", nullable = false)
    private Long paymentSeq;

    /**
     * 주문 취소는 반품 요청 없이 바로 환불하므로 null.
     */
    @Column(name = "return_request")
    private Long returnRequest;

    /**
     * 주문 취소는 반품 배송 없이 바로 환불하므로 null.
     */
    @Column(name = "returns_seq")
    private Long returnsSeq;

    @Column(name = "refund_uid", nullable = false, unique = true, length = 100)
    private String refundUid;

    @Column(name = "refund_quantity", nullable = false)
    private Integer refundQuantity;

    /**
     * 환불 대상 상품 원가.
     * original_price * 환불수량
     */
    @Column(name = "refund_product_price", nullable = false)
    private Integer refundProductPrice;

    @Column(name = "refund_hotdeal", nullable = false)
    private Integer refundHotdeal;

    @Column(name = "refund_coupon", nullable = false)
    private Integer refundCoupon;

    @Column(name = "refund_participation", nullable = false)
    private Integer refundParticipation;

    /**
     * 실제 PG 취소 요청에 포함되는 환불 금액.
     */
    @Column(name = "refund_price", nullable = false)
    private Integer refundPrice;

    /**
     * 예시 상태값:
     * 1: 환불요청
     * 2: 환불완료
     * 3: 환불실패
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "complete_date")
    private LocalDateTime completeDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}