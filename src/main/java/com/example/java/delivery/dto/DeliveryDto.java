package com.example.java.delivery.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.java.delivery.entity.Delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto {

	private Long seq;
	private String tracking_number;
	private String recipient_name;
	private String recipient_phone;
	private String status;
	private String request_memo;
	private LocalDateTime dispatch_at;
	private LocalDateTime estimated_date;
	private LocalDateTime completed_at;
	private Integer distance_surcharge;
	private Integer total_delivery_fee;
	private Integer delayHours;
	
	public Delivery toEntity() {
		
		return Delivery.builder()
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
	// 배송 정보 요약 메서드 예시
	public String toAiSummary() {
	    return String.format("배송 상태: %s, 운송장번호: %s, 수령인: %s", 
	            this.status, this.tracking_number, this.recipient_name);
	}
	
}
