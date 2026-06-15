package com.example.java.adminpayment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.adminpayment.dto.AdminPaymentDTO;
import com.example.java.adminpayment.entity.AdminPayment;
import com.example.java.adminpayment.enums.PaymentType;
import com.example.java.adminpayment.repository.AdminPaymentRepository;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.product.entity.Seller;
import com.example.java.product.repository.SellerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

	private final AdminPaymentRepository adminPaymentRepository;
    private final SellerRepository sellerRepository;
	
	@Transactional
	public void createPurchasePayment(PurchaseOrder order) {

	    // CHECK 제약: purchase_order_seq와 seller_seq 중 하나만 들어가야 함
	    // 발주 대금은 purchase_order_seq만 저장, seller_seq는 NULL
	    AdminPayment payment = AdminPayment.builder()
	            .type(PaymentType.발주)
	            .status(0)
	            .purchaseOrder(order)
	            .build();
	    adminPaymentRepository.save(payment);
	}
	
	public List<AdminPaymentDTO> getList(String keyword) {

        return adminPaymentRepository.findAllByOrderBySeqDesc()
            .stream()
            .map(payment -> {
                PurchaseOrder po = payment.getPurchaseOrder();

                // CHECK 제약 구조:
                // - 발주 대금: purchase_order_seq 있음, seller_seq NULL → po에서 seller 조회
                // - 월배송비 대금: seller_seq 있음, purchase_order_seq NULL → 엔티티에서 직접 접근
                Seller seller = payment.getSeller();
                if (seller == null && po != null
                        && po.getOptions() != null
                        && po.getOptions().getProduct() != null) {
                    seller = sellerRepository
                            .findById(po.getOptions().getProduct().getSellerSeq())
                            .orElse(null);
                }

                String sellerName = seller != null ? seller.getName() : "-";
                String sellerAccount = seller != null ? seller.getAccountNumber() : "-";
                String productName = "-";
                Integer quantity = 0;
                Long supplyPrice = 0L;
                Long totalPrice = 0L;
                Long poSeq = null;

                if (po != null) {
                    poSeq = po.getSeq();
                    quantity = po.getQuantity();
                    supplyPrice = po.getSupplyPrice();
                    totalPrice = po.getTotalPrice();

                    if (po.getOptions() != null && po.getOptions().getProduct() != null) {
                        productName = po.getOptions().getProduct().getProductName();
                    }
                }

                return AdminPaymentDTO.builder()
                    .seq(payment.getSeq())
                    .type(payment.getType())
                    .status(payment.getStatus())
                    .purchaseOrderSeq(poSeq)
                    .sellerName(sellerName)
                    .sellerAccount(sellerAccount)
                    .productName(productName)
                    .quantity(quantity)
                    .supplyPrice(supplyPrice)
                    .totalPrice(totalPrice)
                    .build();
            })
            .filter(dto -> {
                if (keyword == null || keyword.trim().isEmpty()) return true;
                String k = keyword.trim().toLowerCase();
                boolean matchSeller = dto.getSellerName() != null && dto.getSellerName().toLowerCase().contains(k);
                boolean matchProduct = dto.getProductName() != null && dto.getProductName().toLowerCase().contains(k);
                return matchSeller || matchProduct;
            })
            .toList();
    }
    
    @Transactional
    public void changeStatus(Long seq, Integer status) {
        AdminPayment payment = adminPaymentRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment seq"));
        payment.changeStatus(status);
    }
}
