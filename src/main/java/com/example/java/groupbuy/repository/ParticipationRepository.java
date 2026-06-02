package com.example.java.groupbuy.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.Participation;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByGroupBuy(GroupBuy groupBuy);
    List<Participation> findByGroupBuySeq(Long groupBuySeq);
    List<Participation> findByMemberSeq(Long memberSeq);
    List<Participation> findByGroupBuySeqAndMemberSeq(Long groupBuySeq, Long memberSeq);
}
