package com.example.java.product.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.entity.Options;

@SpringBootTest(
    properties = {
        "spring.batch.job.enabled=false"
    }
)
@Transactional
class OptionsServiceTest {

	@Autowired
	private OptionsService optionsService;
	
	@Test
	void 옵션재고변경test() {
		// given
	    Long optionSeq = 1L;
	    int quantity = 10;

	    Options beforeOption = optionsService.findById(optionSeq);
	    int beforeStock = beforeOption.getStock();

	    // when
	    optionsService.increaseStock(optionSeq, quantity);

	    //then
	    Options afterOption = optionsService.findById(optionSeq);
	    assertEquals(beforeStock + quantity, afterOption.getStock());
	}

}
