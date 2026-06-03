package com.example.java.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.java.delivery.entity.Hub;

public interface HubRepository extends JpaRepository<Hub, Long> {
}
