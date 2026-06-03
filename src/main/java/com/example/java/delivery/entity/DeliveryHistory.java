package com.example.java.delivery.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Setter;

@Entity
@Table(name = "delivery_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryHistory {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "delivery_history_seq", allocationSize = 1, sequenceName = "delivery_history_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "delivery_history_seq")
    private Long seq;

    @Column(name = "location", nullable = false, length = 20)
    private String location; // SENDER, HUB, RECEIVER

    @Column(name = "curr_latitude", nullable = false)
    private Double currLatitude;

    @Column(name = "curr_longitude", nullable = false)
    private Double currLongitude;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_seq", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_seq")
    private Hub hub;
}
