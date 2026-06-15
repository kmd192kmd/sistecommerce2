package com.example.java.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.MembershipsInfo;

public interface MembershipsInfoRepository extends JpaRepository<MembershipsInfo, Long> {
}
