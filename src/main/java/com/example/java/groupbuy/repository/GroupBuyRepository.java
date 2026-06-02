package com.example.java.groupbuy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;

public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {
}
