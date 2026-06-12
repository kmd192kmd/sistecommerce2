package com.example.java.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.example.java.delivery.entity.DeliveryCompany;

@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Returns {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "returns_seq_gen")
    @SequenceGenerator(name = "returns_seq_gen", sequenceName = "returns_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_seq", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_company_seq", nullable = false)
    private DeliveryCompany deliveryCompany;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SHIPPING, DELIVERED, CANCELED

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 100)
    private String trackingNumber;
}
