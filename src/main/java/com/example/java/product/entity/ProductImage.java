package com.example.java.product.entity;

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
@Table(name = "product_image")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "product_image_seq", allocationSize = 1, sequenceName = "product_image_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_image_seq")
    private Long seq;

    @Column(name = "product_seq", nullable = false)
    private Long productSeq;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "public_id", length = 500)
    private String publicId;

    @Column(name = "thumbnail_yn", nullable = false, length = 1)
    @Builder.Default
    private String thumbnailYn = "N"; // Y, N

    @Column(name = "image_order", nullable = false)
    @Builder.Default
    private Integer imageOrder = 1;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "NORMAL"; // NORMAL, DELETED
}
