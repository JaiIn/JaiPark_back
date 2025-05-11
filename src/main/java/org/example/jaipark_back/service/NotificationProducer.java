package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationProducer {
    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public CompletableFuture<SendResult<String, NotificationEvent>> sendNotification(NotificationEvent event) {
        return kafkaTemplate.send("notification", event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Message sent successfully: " + result.getRecordMetadata());
                } else {
                    System.err.println("Error sending message: " + ex.getMessage());
                }
            });
    }
} 