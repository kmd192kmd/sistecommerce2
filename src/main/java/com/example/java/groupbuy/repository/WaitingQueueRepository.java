package com.example.java.groupbuy.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.WaitingQueue;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {
    List<WaitingQueue> findByGroupBuyOrderByCreatedAtAsc(GroupBuy groupBuy);
    List<WaitingQueue> findByGroupBuyOptionsOrderByCreatedAtAsc(GroupBuyOptions groupBuyOptions);
    List<WaitingQueue> findByGroupBuySeqAndMemberSeq(Long groupBuySeq, Long memberSeq);
}
