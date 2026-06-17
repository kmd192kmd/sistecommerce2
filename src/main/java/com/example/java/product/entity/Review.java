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
        리뷰 작성자 회원 번호입니다.

        ReviewRepository에서는 member_seq 컬럼을 기준으로
        리뷰 작성자 확인, 수정, 삭제를 처리하고 있습니다.

        따라서 user_seq가 아니라 member_seq로 맞춰야 합니다.
    */
    @Column(name = "member_seq", nullable = false)
    private Long memberSeq;

    /*
        리뷰를 작성한 주문상품 번호입니다.

        ReviewRepository.insertReview()에서
        order_item_seq 컬럼에 값을 INSERT하고 있으므로
        엔티티에도 컬럼을 맞춰줍니다.
    */
    @Column(name = "order_item_seq")
    private Long orderItemSeq;

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