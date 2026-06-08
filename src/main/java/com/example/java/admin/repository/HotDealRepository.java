package com.example.java.admin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.java.admin.hotdeal.Entity.HotDeal;

@Repository
public interface HotDealRepository extends JpaRepository<HotDeal, Long> {
	
	 // 오픈 시간이 지났는데 아직 대기중(0)인 핫딜 찾기
    List<HotDeal> findByStatusAndStartDateLessThanEqual(Integer status, LocalDateTime now);

    // 마감 시간이 지났는데 아직 진행중(1)인 핫딜 찾기
    List<HotDeal> findByStatusAndEndDateLessThanEqual(Integer status, LocalDateTime now);
    
}