package com.example.java.delivery.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.delivery.entity.DeliveryHistory;

public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistory, Long> {
    List<DeliveryHistory> findByDeliverySeqOrderBySeqAsc(Long deliverySeq);
}
