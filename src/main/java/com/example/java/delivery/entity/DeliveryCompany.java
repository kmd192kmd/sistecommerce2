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
@Table(name = "delivery_company")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCompany {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "delivery_company_seq", allocationSize = 1, sequenceName = "delivery_company_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "delivery_company_seq")
    private Long seq;
    
    @Column(name = "name", nullable = false, length = 60)
    private String name;
    
    @Column(name = "customer_service_phone", nullable = false, length = 20)
    private String customer_service_phone;
    
    @Column(name = "base_delivery_fee", nullable = false)
    private Integer base_delivery_fee;
    
    @Column(name = "monthly_fee", nullable = false)
    private Integer monthly_fee;
}
