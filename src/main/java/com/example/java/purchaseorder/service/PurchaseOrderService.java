package com.example.java.purchaseorder.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.service.OptionsService;
import com.example.java.purchaseorder.dto.PurchaseOrderCreateDTO;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;
import com.example.java.purchaseorder.enums.PurchaseOrderType;
import com.example.java.purchaseorder.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final OptionsService optionsService;
	private final GroupBuyOptionsRepository groupBuyOptionsRepository;
	
	@Transactional(readOnly = true)
	public PurchaseOrder findById(Long seq) {
		return getPurchaseOrder(seq);
	}
	
	public Long create(PurchaseOrderCreateDTO dto) {
		Options options = getOptions(dto.getOptionsSeq());
		GroupBuyOptions group = getGroupBuyOptions(dto.getGroupBuyOptionsSeq());

	    PurchaseOrder order = PurchaseOrder.builder()
	    		.status(PurchaseOrderStatus.발주요청)
	            .quantity(dto.getQuantity())
	            .supplyPrice(dto.getSupplyPrice())
	            .totalPrice(dto.getTotalPrice())
	            .orderDate(dto.getOrderDate())
	            .expectedDate(dto.getExpectedDate())
	            .receivedDate(null)
	            .type(dto.getType() != null ? dto.getType() : PurchaseOrderType.일반)
	            .options(options)
	            .groupBuyOptions(group)
	            .build();
	    
	    // TODO 판매처에 알림 주는것 추가해야 함

	    return purchaseOrderRepository.save(order).getSeq();
	}
	
	public void updateStatus(List<Long> seqs, PurchaseOrderStatus status) {

	    List<PurchaseOrder> orders = purchaseOrderRepository.findAllById(seqs);

	    if (orders.size() != seqs.size()) {
	        throw new IllegalArgumentException("존재하지 않는 발주가 있습니다.");
	    }

	    switch (status) {
	    case 입고완료:
	        orders.forEach(this::completeOrder);
	        break;
	    case 물품불량:
	        orders.forEach(this::defectiveOrder);
	        break;
	    case 입고지연:
	        orders.forEach(this::delayOrder);
	        break;
	    case 지연입고:
	        orders.forEach(this::delayedCompleteOrder);
	        break;
	    }
	}
	
	@Transactional(readOnly = true)
	public List<PurchaseOrder> findAll() {
		return purchaseOrderRepository.findAll();
	}
	
	
	
	
	private void completeOrder(PurchaseOrder order) {
	    order.changeStatus(PurchaseOrderStatus.입고완료);
	    // 재고 증가
	    optionsService.increaseStock(order.getOptions().getSeq(), order.getQuantity());
	    
	    // TODO 재고 이력 생성
	    // stockHistoryRepository.createHistory(order);
	    
	    // 입고일(receivedDate) 저장
	    order.changeReceivedDate(LocalDate.now());
	}
	private void defectiveOrder(PurchaseOrder order) {
	    order.changeStatus(PurchaseOrderStatus.물품불량);
	    // 동일 옵션으로 신규 발주 생성 및 예상 입고일 재설정
	    createReOrder(order);
	    
	    // TODO 판매처 알림
	}
	private void delayOrder(PurchaseOrder order) {
	    order.changeStatus(PurchaseOrderStatus.입고지연);
	    // TODO 판매처 알림
	    // TODO 관리자 알림
	}
	private void delayedCompleteOrder(PurchaseOrder order) {
	    order.changeStatus(PurchaseOrderStatus.지연입고);
	    // 재고 증가
	    optionsService.decreaseStock(order.getOptions().getSeq(), order.getQuantity());
	    
	    // TODO 재고 이력 생성
	    // stockHistoryRepository.createHistory(order);
	    
	    // 입고일(receivedDate) 저장
	    order.changeReceivedDate(LocalDate.now());
	}
	
	private PurchaseOrder createReOrder(PurchaseOrder originalOrder) {

	    long diffDays =
	            ChronoUnit.DAYS.between(
	                    originalOrder.getOrderDate(),
	                    originalOrder.getExpectedDate());

	    LocalDate newOrderDate = LocalDate.now();

	    LocalDate newExpectedDate =
	            newOrderDate.plusDays(diffDays);

	    PurchaseOrder reOrder = PurchaseOrder.builder()
	            .status(PurchaseOrderStatus.발주요청)
	            .quantity(originalOrder.getQuantity())
	            .supplyPrice(originalOrder.getSupplyPrice())
	            .totalPrice(originalOrder.getTotalPrice())
	            .orderDate(newOrderDate)
	            .expectedDate(newExpectedDate)
	            .receivedDate(null)
	            .type(originalOrder.getType())
	            .options(originalOrder.getOptions())
	            .groupBuyOptions(originalOrder.getGroupBuyOptions())
	            .build();

	    return purchaseOrderRepository.save(reOrder);
	}
	
	private PurchaseOrder getPurchaseOrder(Long seq) {
		if (seq == null) {
	        throw new IllegalArgumentException("purchaseOrderSeq is required");
	    }
	    return purchaseOrderRepository.findById(seq)
	            .orElseThrow(() -> new IllegalArgumentException("purchaseOrder not found"));
	}
	
	private Options getOptions(Long seq) {
		if (seq == null) {
			throw new IllegalArgumentException("optionsSeq is required");
		}
		return optionsService.findById(seq);
	}
	
	private GroupBuyOptions getGroupBuyOptions(Long seq) {
	    if (seq == null) return null;
	    return groupBuyOptionsRepository.findById(seq)
	            .orElseThrow(() -> new IllegalArgumentException("GroupBuyOptions not found"));
	}
}
