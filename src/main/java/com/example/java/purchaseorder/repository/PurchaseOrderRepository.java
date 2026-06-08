package com.example.java.purchaseorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.java.purchaseorder.entity.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	@Query("""
		    select po
		    from PurchaseOrder po
		    join fetch po.options
		    order by po.seq desc
		""")
		List<PurchaseOrder> findAllWithOptions();
}
