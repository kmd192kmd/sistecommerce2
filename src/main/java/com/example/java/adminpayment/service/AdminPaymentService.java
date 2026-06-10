package com.example.java.adminpayment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.adminpayment.dto.AdminPaymentDTO;
import com.example.java.adminpayment.entity.AdminPayment;
import com.example.java.adminpayment.enums.PaymentType;
import com.example.java.adminpayment.repository.AdminPaymentRepository;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.product.entity.Product;
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
                String sellerName = "-";
                String sellerAccount = "-";
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
                        Product product = po.getOptions().getProduct();
                        productName = product.getProductName();
                        
                        Seller seller = sellerRepository.findById(product.getSellerSeq()).orElse(null);
                        if (seller != null) {
                            sellerName = seller.getName();
                            sellerAccount = seller.getAccountNumber();
                        }
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
