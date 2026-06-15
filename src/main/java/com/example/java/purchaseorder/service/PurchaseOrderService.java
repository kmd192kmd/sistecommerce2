package com.example.java.purchaseorder.service;

import java.time.LocalDate;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.adminpayment.service.AdminPaymentService;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.service.OptionsService;
import com.example.java.purchaseorder.dto.PurchaseOrderCreateDTO;
import com.example.java.purchaseorder.dto.PurchaseOrderListDTO;
import com.example.java.purchaseorder.dto.PurchaseOrderSearchDTO;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;
import com.example.java.purchaseorder.enums.PurchaseOrderType;
import com.example.java.purchaseorder.repository.PurchaseOrderQueryDslRepository;
import com.example.java.purchaseorder.repository.PurchaseOrderRepository;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.service.StockHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderQueryDslRepository queryDslRepository;
	private final OptionsService optionsService;
	private final GroupBuyOptionsRepository groupBuyOptionsRepository;
	private final AdminPaymentService adminPaymentService;
	private final StockHistoryService stockHistoryService;
	private final com.example.java.product.repository.SellerRepository sellerRepository;
	
	@Transactional(readOnly = true)
	public PurchaseOrder findById(Long seq) {
		return getPurchaseOrder(seq);
	}
	
	@Transactional(readOnly = true)
	public java.util.Map<String, Object> getCreateFormData(Long optionsSeq) {
		Options options = getOptions(optionsSeq);
		com.example.java.product.entity.Product product = options.getProduct();
		com.example.java.product.entity.Seller seller = sellerRepository.findById(product.getSellerSeq())
				.orElseThrow(() -> new IllegalArgumentException("Seller not found"));

		long totalOptionPrice = product.getPrice() + options.getAdditionalPrice();
		long supplyPrice = (long) (totalOptionPrice * (seller.getSupplyRate() / 100.0));

		java.util.Map<String, Object> map = new java.util.HashMap<>();
		map.put("options", options);
		map.put("product", product);
		map.put("seller", seller);
		map.put("supplyPrice", supplyPrice);
		return map;
	}

	public Long create(PurchaseOrderCreateDTO dto) {
		Options options = getOptions(dto.getOptionsSeq());
		GroupBuyOptions group = getGroupBuyOptions(dto.getGroupBuyOptionsSeq());

		com.example.java.product.entity.Product product = options.getProduct();
		com.example.java.product.entity.Seller seller = sellerRepository.findById(product.getSellerSeq())
				.orElseThrow(() -> new IllegalArgumentException("Seller not found"));

		long totalOptionPrice = product.getPrice() + options.getAdditionalPrice();
		long calculatedSupplyPrice = (long) (totalOptionPrice * (seller.getSupplyRate() / 100.0));
		long calculatedTotalPrice = calculatedSupplyPrice * dto.getQuantity();

	    PurchaseOrder order = PurchaseOrder.builder()
	    		.status(PurchaseOrderStatus.발주요청)
	            .quantity(dto.getQuantity())
	            .supplyPrice(calculatedSupplyPrice)
	            .totalPrice(calculatedTotalPrice)
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
		return purchaseOrderRepository.findAllWithOptions();
	}
	
	@Transactional(readOnly = true)
	public Slice<PurchaseOrderListDTO> getList(PurchaseOrderSearchDTO search, Pageable pageable) {
		
		List<PurchaseOrder> contents =
	            queryDslRepository
	                    .findAllWithOptionsAndProduct(search, pageable);

	    boolean hasNext =
	            contents.size() > pageable.getPageSize();

	    if (hasNext) {
	        contents.remove(contents.size() - 1);
	    }

	    // 추가: N+1 문제를 방지하기 위해 화면에 보여질 상품의 판매자 번호를 추출하여 한 번에 조회
	    List<Long> sellerSeqs = contents.stream()
	            .map(po -> po.getOptions().getProduct().getSellerSeq())
	            .distinct()
	            .toList();

	    java.util.Map<Long, String> sellerNameMap = sellerRepository.findAllById(sellerSeqs).stream()
	            .collect(java.util.stream.Collectors.toMap(
	            		com.example.java.product.entity.Seller::getSeq,
	            		com.example.java.product.entity.Seller::getName
	            ));

	    List<PurchaseOrderListDTO> dtoList =
	            contents.stream()
	                    .map(po -> {
	                    	Long sellerSeq = po.getOptions().getProduct().getSellerSeq();
	                    	String sellerName = sellerNameMap.getOrDefault(sellerSeq, "알 수 없음");
	                    	return PurchaseOrderListDTO.from(po, sellerName);
	                    })
	                    .toList();

	    return new SliceImpl<>(
	            dtoList,
	            pageable,
	            hasNext
	    );
	}
	
	
	
	
	
	private void completeOrder(PurchaseOrder order) {
	    order.changeStatus(PurchaseOrderStatus.입고완료);
	    // 재고 증가
	    optionsService.increaseStock(order.getOptions().getSeq(), order.getQuantity());
	    
	    // 재고 이력 생성
	    stockHistoryService.createInStockHistory(
	    		order.getOptions(), order.getQuantity(),
	    		StockHistorySourceType.발주, "입고완료");
	    
	    // 입고일(receivedDate) 저장
	    order.changeReceivedDate(LocalDate.now());
	    
	    // 대금 정보 생성
	    adminPaymentService.createPurchasePayment(order);
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
	    optionsService.increaseStock(order.getOptions().getSeq(), order.getQuantity());
	    
	    // 재고 이력 생성
	    stockHistoryService.createInStockHistory(
	    		order.getOptions(), order.getQuantity(),
	    		StockHistorySourceType.발주, "입고완료");
	    
	    // 입고일(receivedDate) 저장
	    order.changeReceivedDate(LocalDate.now());
	    
	    // 대금 정보 생성
	    adminPaymentService.createPurchasePayment(order);
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
