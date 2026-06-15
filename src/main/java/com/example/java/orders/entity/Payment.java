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
@Table(name = "payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(
            name = "payment_seq_generator",
            sequenceName = "payment_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq_generator")
    private Long seq;

    @Column(name = "order_seq", nullable = false)
    private Long orderSeq;

    @Column(name = "payment_uid", nullable = false, unique = true, length = 100)
    private String paymentUid;

    @Column(name = "external_payment_id", length = 200)
    private String externalPaymentId;

    @Column(name = "pg_tid", length = 200)
    private String pgTid;

    /**
     * 임시 매핑:
     * 0: 카드
     * 1: 계좌이체
     * 2: 가상계좌
     */
    @Column(name = "payment_method", nullable = false)
    private Integer paymentMethod;

    @Column(name = "pg_provider", nullable = false, length = 50)
    private String pgProvider;

    /**
     * 0: 결제준비
     * 1: 가상계좌입금대기
     * 2: 결제완료
     * 3: 결제실패
     * 4: 결제취소
     * 5: 환불완료
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    @Column(name = "receipt_url", length = 1000)
    private String receiptUrl;

    @Column(name = "fail_reason", length = 1000)
    private String failReason;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}