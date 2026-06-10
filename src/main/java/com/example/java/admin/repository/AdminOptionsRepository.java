package com.example.java.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.product.entity.Options;

public interface AdminOptionsRepository extends JpaRepository<Options, Long> {
	 List<Options> findByProductSeq(Long productSeq);
	
	 @Query("SELECT o FROM Options o JOIN FETCH o.product p WHERE p.productName LIKE %:keyword%")
     List<Options> searchByProductName(@Param("keyword") String keyword);

}
