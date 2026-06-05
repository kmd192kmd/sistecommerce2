package com.example.java.purchaseorder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;
import com.example.java.purchaseorder.repository.PurchaseOrderRepository;
import com.example.java.purchaseorder.service.PurchaseOrderService;

@SpringBootTest(
    properties = {
        "spring.batch.job.enabled=false"
    }
)
//@Transactional
public class PurchaseOrderServiceTest {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

//    @Test
//    void test() {
//    	List<Long> seqs = List.of(3L, 4L);
//    	List<PurchaseOrder> orders = purchaseOrderRepository.findAllById(seqs);
//    	for (PurchaseOrder order : orders) {
//            System.out.println(order.getStatus());
//            System.out.println(order.getReceivedDate());
//        }
//    	
//    }
    
//    @Test
//    void 발주상태일괄변경test() {
//
//        List<Long> seqs = List.of(3L, 4L);
//        List<PurchaseOrder> orders = purchaseOrderRepository.findAllById(seqs);
//        purchaseOrderService.updateStatus(seqs, PurchaseOrderStatus.지연입고);
//        for (PurchaseOrder order : orders) {
//        	assertEquals(PurchaseOrderStatus.지연입고, order);
////            System.out.println(order.getStatus());
//            System.out.println(order.getReceivedDate());
//        }
//    }
    
//    @Test
//    void 발주상태변경test() {
//    	
//    	purchaseOrderService.updateStatus(List.of(3L), PurchaseOrderStatus.입고완료);
//    	PurchaseOrder order = purchaseOrderRepository.findById(3L).get();
//    	System.out.println(order.getStatus());
//    	System.out.println(order.getReceivedDate());
//
//	}
    
//    @Test
//    void 발주등록test() {
//
//        PurchaseOrderCreateDTO dto = PurchaseOrderCreateDTO.builder()
//                .optionsSeq(1L)   // DB에 실제 존재하는 FK
//                .quantity(10)
//                .supplyPrice(200L)
//                .totalPrice(2000L)
//                .orderDate(new Date(126, 5, 4))      // 2026-06-04
//                .expectedDate(new Date(126, 5, 10))  // 2026-06-10
//                .build();
//
//        Long seq = purchaseOrderService.create(dto);
//
//        System.out.println("생성된 발주번호 = " + seq);
//
//        PurchaseOrder order = purchaseOrderRepository.findById(seq)
//                .orElseThrow();
//
//        System.out.println("상태 = " + order.getStatus());
//        System.out.println("타입 = " + order.getType());
//        System.out.println("수량 = " + order.getQuantity());
//        System.out.println("옵션번호 = " + order.getOptions().getSeq());
//    }
}