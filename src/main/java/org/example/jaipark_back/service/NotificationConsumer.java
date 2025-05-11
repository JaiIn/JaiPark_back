package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {
    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "notification", groupId = "notification-group")
    public void consume(NotificationEvent event, Acknowledgment ack) {
        try {
            notificationService.saveNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            // 에러 발생 시 로깅
            System.err.println("Error processing notification: " + e.getMessage());
            throw e; // 에러를 다시 던져서 DefaultErrorHandler가 처리하도록 함
        }
    }
} 