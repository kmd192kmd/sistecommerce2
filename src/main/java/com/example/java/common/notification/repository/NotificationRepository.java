package com.example.java.common.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.common.notification.entity.Notification;
import com.example.java.common.notification.entity.NotificationRecipientType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(
            NotificationRecipientType recipientType, Long recipientSeq);

    long countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
            NotificationRecipientType recipientType, Long recipientSeq);
}
