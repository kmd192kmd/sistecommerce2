package com.example.java.orders.controller.entity;

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
    @SequenceGenerator(name = "orders_seq", allocationSize = 1, sequenceName = "orders_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    private Long seq;

    @Column(name = "member_seq", nullable = false)
    private Long memberSeq;

    @Column(name = "order_item_seq", nullable = false)
    private Long orderItemSeq;

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

    @Column(name = "order_status", nullable = false)
    @Builder.Default
    private Integer orderStatus = 0;

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
