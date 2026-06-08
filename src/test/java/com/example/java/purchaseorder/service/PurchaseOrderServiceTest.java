package com.example.java.purchaseorder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.entity.Options;
import com.example.java.product.service.OptionsService;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;

@SpringBootTest(
	properties = {
        "spring.batch.job.enabled=false"
    }
)
@Transactional
class PurchaseOrderServiceTest {

	@Autowired
	PurchaseOrderService purchaseOrderService;
	@Autowired
	OptionsService optionsService;
	
	@Test
	void 발주상태변경시재고증가test() {
	    // given
	    PurchaseOrder order = purchaseOrderService.findById(3L);
	    Long optionSeq = order.getOptions().getSeq();
	    int quantity = order.getQuantity();
	    int beforeStock = optionsService.findById(optionSeq).getStock();

	    // when
	    purchaseOrderService.updateStatus(
	            List.of(order.getSeq()),
	            PurchaseOrderStatus.입고완료
	    );

	    // then
	    Options updatedOption = optionsService.findById(optionSeq);
	    assertEquals(beforeStock + quantity, updatedOption.getStock());
	}

}
