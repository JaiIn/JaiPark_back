package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {
    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "notification", groupId = "notification-group")
    public void consume(NotificationEvent event) {
        notificationService.saveNotification(event);
    }
} 