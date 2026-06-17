package com.example.java.productrequest.entity;

import java.time.LocalDate;

import com.example.java.admin.entity.Admin;
import com.example.java.product.entity.Product;
import com.example.java.product.entity.Seller;
import com.example.java.productrequest.enums.ProductRequestStatus;
import com.example.java.productrequest.enums.ProductRequestType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "product_request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_request_seq")
    @SequenceGenerator(
            name = "product_request_seq",
            sequenceName = "product_request_seq",
            allocationSize = 1
    )
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_seq", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_seq", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_seq")
    private Admin admin;

    // REGISTER / UPDATE
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private ProductRequestType requestType;

    // PENDING / APPROVED / REJECTED
    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 20)
    private ProductRequestStatus requestStatus;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "process_date")
    private LocalDate processDate;
    
    public void approve(Admin admin) {
        this.admin = admin;
        this.requestStatus = ProductRequestStatus.APPROVED;
        this.processDate = LocalDate.now();
        this.rejectReason = null;
    }

    public void reject(Admin admin, String rejectReason) {

        if (rejectReason == null || rejectReason.isBlank()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다.");
        }

        this.admin = admin;
        this.requestStatus = ProductRequestStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.processDate = LocalDate.now();
    }
}
