package com.example.java.product.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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

/**
 * 상품 찜 Entity
 *
 * product_wish 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "product_wish")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWish {

    /*
        찜 고유번호
        product_wish.seq 컬럼과 매핑됩니다.
    */
    @Id
    @Column(name = "seq")
    @SequenceGenerator(
            name = "product_wish_seq_generator",
            sequenceName = "product_wish_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_wish_seq_generator"
    )
    private Long seq;

    /*
        상품 번호
        product_wish.product_seq 컬럼과 매핑됩니다.
    */
    @Column(name = "product_seq", nullable = false)
    private Long productSeq;

    /*
        회원 번호
        product_wish.member_seq 컬럼과 매핑됩니다.
    */
    @Column(name = "member_seq", nullable = false)
    private Long memberSeq;

    /*
        찜 등록일
        INSERT 시 자동으로 현재 시간이 들어갑니다.
    */
    @CreationTimestamp
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    /**
     * 찜 상태
     *
     * NORMAL  = 찜한 상태
     * DELETED = 찜 취소 상태
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;
}