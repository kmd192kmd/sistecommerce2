package com.example.java.product.entity;

import java.time.LocalDateTime;

import com.example.java.delivery.entity.Delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seller")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seller_seq")
    @SequenceGenerator(name = "seller_seq", sequenceName = "seller_seq", allocationSize = 1)
    private Long seq;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "zipcode", nullable = false, length = 100)
    private String zipcode;

    @Column(name = "address", nullable = false, length = 100)
    private String address;

    @Column(name = "address_detail", length = 100)
    private String addressDetail;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "status", nullable = false)
    private Integer status;	// 1: 활성 2:휴면 3:일시정지 4:탈퇴보류중 5: 탈퇴

    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "supply_rate", nullable = false)
    private Integer supplyRate;	// 판매가 기준 공급가 계산 비율. (예: 100원 상품, 60% → 공급가 60원)

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;	// 계좌번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_seq", nullable = false)
    private Delivery delivery;
}