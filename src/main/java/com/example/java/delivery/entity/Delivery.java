package com.example.java.delivery.entity;

import java.time.LocalDateTime;
import com.example.java.delivery.dto.DeliveryDto;
import com.example.java.orders.entity.Orders;
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
@Table(name = "delivery")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {
    
    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "delivery_seq", allocationSize = 1, sequenceName = "delivery_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "delivery_seq")
    private Long seq;
    
    @Column(name = "tracking_number", nullable = false, unique = true, length = 100)
    private String tracking_number;
    
    @Column(name = "recipient_name", nullable = false, length = 60)
    private String recipient_name;
    
    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipient_phone;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status; // READY, SHIPPING, DELIVERED, FAILED, DELAYED, CANCELED
    
    @Column(name = "request_memo", length = 255)
    private String request_memo;
    
    @Column(name = "dispatch_at")
    private LocalDateTime dispatch_at;
    
    @Column(name = "estimated_date")
    private LocalDateTime estimated_date;
    
    @Column(name = "completed_at")
    private LocalDateTime completed_at;
    
    @Column(name = "distance_surcharge", nullable = false)
    @Builder.Default
    private Integer distance_surcharge = 0;
    
    @Column(name = "total_delivery_fee", nullable = false)
    private Integer total_delivery_fee;
    
    @Column(name = "delay_hours", nullable = false)
    @Builder.Default
    private Integer delayHours = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_company_seq", nullable = false)
    private DeliveryCompany deliveryCompany;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_seq")
    private Orders orders;
    
    @Column(name = "purchase_order_seq")
    private Long purchaseOrderSeq;
    
    public DeliveryDto toDto() {
        return DeliveryDto.builder()
                       .seq(this.seq)
                       .tracking_number(this.tracking_number)
                       .recipient_name(this.recipient_name)
                       .recipient_phone(this.recipient_phone)
                       .status(this.status)
                       .request_memo(this.request_memo)
                       .dispatch_at(this.dispatch_at)
                       .estimated_date(this.estimated_date)
                       .completed_at(this.completed_at)
                       .distance_surcharge(this.distance_surcharge)
                       .total_delivery_fee(this.total_delivery_fee)
                       .delayHours(this.delayHours)
                       .build();
    }
    
    public String getTrackingNumber() {
        return this.tracking_number;
    }
}
