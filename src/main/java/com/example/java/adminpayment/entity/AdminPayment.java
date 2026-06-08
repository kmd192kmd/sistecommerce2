package com.example.java.adminpayment.entity;


import com.example.java.adminpayment.enums.PaymentType;
import com.example.java.purchaseorder.entity.PurchaseOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_payment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_payment_seq")
    @SequenceGenerator(
            name = "admin_payment_seq",
            sequenceName = "admin_payment_seq",
            allocationSize = 1
    )
    private Long seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PaymentType type;

    @Column(name = "status", nullable = false)
    private Integer status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_seq")
    private PurchaseOrder purchaseOrder;

    // TODO 판매처 엔티티 생기면 추가
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "seller_seq")
//    private Seller seller;

    public void changeStatus(Integer status) {
        this.status = status;
    }
}