package com.example.java.member.repository;

import com.example.java.member.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findByMember_Seq(Long memberSeq);
}
