package com.example.java.groupbuy.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;

public interface GroupBuyOptionsRepository extends JpaRepository<GroupBuyOptions, Long> {
    List<GroupBuyOptions> findByGroupBuy(GroupBuy groupBuy);
    List<GroupBuyOptions> findByGroupBuySeq(Long groupBuySeq);
}
