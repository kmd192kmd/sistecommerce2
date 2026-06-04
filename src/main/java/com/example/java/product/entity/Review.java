package com.example.java.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "review_seq", allocationSize = 1, sequenceName = "review_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_seq")
    private Long seq;

    /*
        리뷰 N : 상품 1

        Review 여러 개가 Product 하나에 속함
        그래서 Review 입장에서는 @ManyToOne
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_seq", nullable = false)
    private Product product;

    /*
        리뷰 작성자 회원 번호

        아직 Member/User 엔티티 연관관계가 확실하지 않으면
        일단 Long 타입으로 두는 게 안전함
    */
    @Column(name = "user_seq", nullable = false)
    private Long userSeq;

    /*
        별점
        1점 ~ 5점
    */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /*
        리뷰 내용
    */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /*
        숨김 여부
        N: 보임
        Y: 숨김
    */
    @Column(name = "hide_yn", nullable = false, length = 1)
    @Builder.Default
    private String hideYn = "N";

    /*
        상태
        NORMAL: 정상
        DELETED: 삭제
    */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "NORMAL";

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();

        if (this.hideYn == null) {
            this.hideYn = "N";
        }

        if (this.status == null) {
            this.status = "NORMAL";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}