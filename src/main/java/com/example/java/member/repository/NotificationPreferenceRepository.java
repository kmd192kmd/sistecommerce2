package com.example.java.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.NotificationPreference;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
}
