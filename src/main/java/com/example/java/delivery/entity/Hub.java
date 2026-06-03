package com.example.java.delivery.entity;

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
@Table(name = "hub")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hub {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "hub_seq", allocationSize = 1, sequenceName = "hub_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hub_seq")
    private Long seq;

    @Column(name = "name", nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "zip_code", nullable = false, length = 5)
    private String zipCode;

    @Column(name = "road_address", nullable = false, length = 200)
    private String roadAddress;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;
}
