package com.example.java.purchaseorder.entity;

import java.time.LocalDate;

import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.product.entity.Options;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;
import com.example.java.purchaseorder.enums.PurchaseOrderType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_order_seq")
    @SequenceGenerator(name = "purchase_order_seq", sequenceName = "purchase_order_seq", allocationSize = 1)
	private Long seq;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderStatus status;
	
	@Min(1)
	@Column(name = "quantity", nullable = false)
    private int quantity;
	
	@Column(name = "supply_price", nullable = false)
    private Long supplyPrice;
	
	@Column(name = "total_price", nullable = false)
	private Long totalPrice;
	
	@Column(name = "order_date", nullable = false)
	private LocalDate orderDate;
	
	@Column(name = "expected_date", nullable = false)
	private LocalDate expectedDate;
	
	@Column(name = "received_date")
	private LocalDate receivedDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private PurchaseOrderType type;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "options_seq")
	private Options options;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_buy_options_seq", nullable = true)
	private GroupBuyOptions groupBuyOptions;
	
	public void changeStatus(PurchaseOrderStatus status) {
	    this.status = status;
	}
	public void changeReceivedDate(LocalDate receivedDate) {
		this.receivedDate = receivedDate;
	}
}
