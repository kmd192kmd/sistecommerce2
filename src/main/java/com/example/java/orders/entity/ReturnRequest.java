package com.example.java.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "return_request_seq_gen")
    @SequenceGenerator(name = "return_request_seq_gen", sequenceName = "return_request_seq", allocationSize = 1)
    private Long seq;

    @Column(name = "order_item_seq", nullable = false)
    private Long orderItemSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_reason_seq", nullable = false)
    private RefundReason refundReason;

    @Column(name = "returnuid", nullable = false, unique = true, length = 100)
    private String returnUid;

    @Column(name = "return_name", nullable = false, length = 100)
    private String returnName;

    @Column(name = "return_quantity", nullable = false)
    @Builder.Default
    private Integer returnQuantity = 1;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 0; // 0: 반품신청, 9: 반품완료

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "decision_date")
    private LocalDateTime decisionDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "reject_reason", length = 100)
    private String rejectReason;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(name = "road_address", nullable = false, length = 200)
    private String roadAddress;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;
}

