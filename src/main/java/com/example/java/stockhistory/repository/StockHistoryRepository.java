package com.example.java.stockhistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.stockhistory.entity.StockHistory;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

}
