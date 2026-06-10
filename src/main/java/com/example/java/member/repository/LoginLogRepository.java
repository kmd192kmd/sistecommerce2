package com.example.java.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.LoginLog;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
