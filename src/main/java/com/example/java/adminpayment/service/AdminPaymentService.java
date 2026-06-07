package com.example.java.adminpayment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.adminpayment.dto.AdminPaymentDTO;
import com.example.java.adminpayment.entity.AdminPayment;
import com.example.java.adminpayment.enums.PaymentType;
import com.example.java.adminpayment.repository.AdminPaymentRepository;
import com.example.java.purchaseorder.entity.PurchaseOrder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

	private final AdminPaymentRepository adminPaymentRepository;
	
	@Transactional
	public void createPurchasePayment(PurchaseOrder order) {

	    AdminPayment payment = AdminPayment.builder()
	            .type(PaymentType.발주)
	            .status(0)
	            .purchaseOrder(order)
	            .build();
	    adminPaymentRepository.save(payment);
	}
	
	public List<AdminPaymentDTO> getList() {

        return adminPaymentRepository.findAllByOrderBySeqDesc()
            .stream()
            .map(payment -> AdminPaymentDTO.builder()
                    .seq(payment.getSeq())
                    .type(payment.getType())
                    .status(payment.getStatus())
                    .purchaseOrderSeq(
                            payment.getPurchaseOrder() != null
                                    ? payment.getPurchaseOrder().getSeq()
                                    : null
                    )
                    .build())
            .toList();
    }
}
