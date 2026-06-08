package com.example.java.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.java.admin.hotdeal.Entity.HotDeal;

@Repository
public interface HotDealRepository extends JpaRepository<HotDeal, Long> {
}