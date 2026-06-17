package com.example.java.member.entity;

import java.time.LocalDate;

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

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coupon_seq")
	@SequenceGenerator(name = "coupon_seq", sequenceName = "coupon_seq",  allocationSize = 1)
	private Long seq;
	
	@Column(name = "name", nullable = false, length = 100)
	private String name;
	
	@Column(name = "discount_type", nullable = false)
	private Integer discountType;
	
	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;
	
	@Column(name = "valid_days", nullable = false)
	private Integer validDays;
	
	@Column(name = "status", nullable = false)
	private Integer status;
	
	@Column(name = "discount_price")
	private Integer discountPrice;
	
	@Column(name = "discount_rate")
	private Integer discountRate;
	
	@Column(name = "expire_date", insertable=false, updatable=false)
	private LocalDate expireDate;
	
	 public void updateStatus(int status) {
	        this.status = status;
	    }
	 
	 public void update(String name, Integer discountType, Integer discountPrice, Integer discountRate, java.time.
	  LocalDate startDate, Integer validDays) {
	            this.name = name;
	            this.discountType = discountType;
	            this.discountPrice = discountPrice;
	            this.discountRate = discountRate;
	            this.startDate = startDate;
	            this.validDays = validDays;
	        }
}
