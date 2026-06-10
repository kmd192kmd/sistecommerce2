package com.example.java.purchaseorder.service;



import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.purchaseorder.dto.InventoryListDTO;
import com.example.java.purchaseorder.dto.InventorySearchDTO;
import com.example.java.purchaseorder.repository.InventoryQueryDslRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

	private final InventoryQueryDslRepository queryDslRepository;
	
	@Transactional(readOnly = true)
	public Slice<InventoryListDTO> getListWithCond(
	        InventorySearchDTO search,
	        Pageable pageable
	) {

		List<InventoryListDTO> contents =
		        new ArrayList<>(
		                queryDslRepository.findAllWithSearchCond(
		                        search,
		                        pageable
		                )
		        );

	    boolean hasNext =
	            contents.size() > pageable.getPageSize();

	    if (hasNext) {
	        contents.remove(contents.size() - 1);
	    }

	    return new SliceImpl<>(
	            contents,
	            pageable,
	            hasNext
	    );
	}
}
