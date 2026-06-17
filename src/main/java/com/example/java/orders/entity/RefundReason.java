package com.example.java.orders.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refund_reason")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundReason {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refund_reason_seq_gen")
    @SequenceGenerator(name = "refund_reason_seq_gen", sequenceName = "refund_reason_seq", allocationSize = 1)
    private Long seq;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;
}
