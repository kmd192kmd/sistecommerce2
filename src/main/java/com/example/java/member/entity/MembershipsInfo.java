package com.example.java.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * memberships_info: 멤버십 상품 정보 (월 가격 등).
 * 현재는 단일 상품(seq=1)만 사용.
 */
@Entity
@Table(name = "memberships_info")
@Getter
@NoArgsConstructor
public class MembershipsInfo {

    @Id
    private Long seq;

    @Column(name = "price", nullable = false)
    private Integer price;
}
