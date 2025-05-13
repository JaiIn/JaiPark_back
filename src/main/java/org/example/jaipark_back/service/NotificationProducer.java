package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationProducer {
    private static final String NOTIFICATION_TOPIC = "notification";
    private static final String LIKE_TOPIC = "notification-like";
    private static final String COMMENT_TOPIC = "notification-comment";
    private static final String FOLLOW_TOPIC = "notification-follow";
    
    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    
    @Autowired
    private KafkaTemplate<String, List<NotificationEvent>> batchKafkaTemplate;

    /**
     * 알림 이벤트를 기본 토픽으로 전송합니다.
     */
    @Async
    public CompletableFuture<SendResult<String, NotificationEvent>> sendNotification(NotificationEvent event) {
        return kafkaTemplate.send(NOTIFICATION_TOPIC, event.getUsername(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Message sent successfully: " + result.getRecordMetadata());
                } else {
                    System.err.println("Error sending message: " + ex.getMessage());
                }
            });
    }
    
    /**
     * 알림 이벤트를 타입에 맞는 토픽으로 전송합니다.
     */
    @Async
    public CompletableFuture<SendResult<String, NotificationEvent>> sendTypedNotification(NotificationEvent event) {
        String topic;
        
        switch (event.getType()) {
            case "LIKE":
                topic = LIKE_TOPIC;
                break;
            case "COMMENT":
                topic = COMMENT_TOPIC;
                break;
            case "FOLLOW":
                topic = FOLLOW_TOPIC;
                break;
            default:
                topic = NOTIFICATION_TOPIC;
        }
        
        return kafkaTemplate.send(topic, event.getUsername(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Message sent to " + topic + " successfully: " + result.getRecordMetadata());
                } else {
                    System.err.println("Error sending message to " + topic + ": " + ex.getMessage());
                }
            });
    }
    
    /**
     * 여러 알림을 일괄적으로 처리합니다.
     */
    @Async
    public CompletableFuture<SendResult<String, List<NotificationEvent>>> sendBatchNotifications(
            String key, List<NotificationEvent> events) {
        return batchKafkaTemplate.send(NOTIFICATION_TOPIC + "-batch", key, events)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Batch sent successfully: " + result.getRecordMetadata());
                } else {
                    System.err.println("Error sending batch: " + ex.getMessage());
                }
            });
    }
    
    /**
     * 알림 이벤트 전송을 재시도합니다. (실패 시)
     */
    @Async
    public CompletableFuture<SendResult<String, NotificationEvent>> retryNotification(
            NotificationEvent event, int retryCount) {
        return kafkaTemplate.send(NOTIFICATION_TOPIC + "-retry", event.getUsername(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Retry sent successfully: " + result.getRecordMetadata());
                } else {
                    if (retryCount > 0) {
                        System.out.println("Retrying... attempts left: " + (retryCount - 1));
                        retryNotification(event, retryCount - 1);
                    } else {
                        System.err.println("Max retries reached. Message lost: " + event);
                    }
                }
            });
    }
}