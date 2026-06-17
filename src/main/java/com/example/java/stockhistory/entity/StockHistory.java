package com.example.java.stockhistory.entity;

import java.time.LocalDateTime;

import com.example.java.product.entity.Options;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.enums.StockHistoryType;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_history_seq")
	@SequenceGenerator(name = "stock_history_seq", sequenceName = "stock_history_seq", allocationSize = 1)
	private Long seq;

	// IN / OUT
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 60)
	private StockHistoryType type;

	// 상세 변동 사유
	@Column(name = "reason", nullable = false, length = 100)
	private String reason;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "before_stock", nullable = false)
	private int beforeStock;

	@Column(name = "after_stock", nullable = false)
	private int afterStock;

	// 주문/발주/관리자 등 참조 id
	@Column(name = "source_seq")
	private Long sourceSeq;

	// 주문, 발주, 반품, 관리자, 상품옵션, 핫딜, 공동구매
	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", length = 60)
	private StockHistorySourceType sourceType;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "options_seq", nullable = false)
	private Options options;
}
