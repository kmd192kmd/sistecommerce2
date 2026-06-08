package com.example.java.admin.repository;

import com.example.java.member.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository<다룰 엔티티 클래스, 그 엔티티의 PK 데이터 타입>
@Repository
public interface AdminCouponRepository extends JpaRepository<Coupon, Long> {

	
	
	java.util.List<Coupon> findByStatusAndStartDateLessThanEqual(Integer status, java.time.LocalDate startDate);
    // 기본적인 save(), findById(), findAll(), deleteById() 등은 
    // JpaRepository를 상속받는 것만으로 이미 다 만들어져 있습니다!
    
}