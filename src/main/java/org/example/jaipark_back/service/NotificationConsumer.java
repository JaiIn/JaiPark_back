package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationProducer notificationProducer;

    /**
     * 기본 알림 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "notification", groupId = "notification-group")
    public void consume(NotificationEvent event, Acknowledgment ack) {
        try {
            logger.info("Received notification: {}", event);
            notificationService.saveNotification(event);
            ack.acknowledge();
            logger.info("Processed notification: {}", event);
        } catch (Exception e) {
            logger.error("Error processing notification: {}", e.getMessage(), e);
            // 오류 발생 시 재시도 토픽으로 보냄
            notificationProducer.retryNotification(event, 3)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        ack.acknowledge(); // 재시도 큐로 전송 성공 시 승인
                    }
                });
        }
    }
    
    /**
     * 좋아요 관련 알림 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "notification-like", groupId = "notification-like-group")
    public void consumeLikeNotification(NotificationEvent event, Acknowledgment ack) {
        try {
            logger.info("Received like notification: {}", event);
            // 좋아요 알림에 특화된 처리 로직 추가 가능
            notificationService.saveNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing like notification: {}", e.getMessage(), e);
            notificationProducer.retryNotification(event, 3);
        }
    }
    
    /**
     * 댓글 관련 알림 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "notification-comment", groupId = "notification-comment-group")
    public void consumeCommentNotification(NotificationEvent event, Acknowledgment ack) {
        try {
            logger.info("Received comment notification: {}", event);
            // 댓글 알림에 특화된 처리 로직 추가 가능
            notificationService.saveNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing comment notification: {}", e.getMessage(), e);
            notificationProducer.retryNotification(event, 3);
        }
    }
    
    /**
     * 팔로우 관련 알림 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "notification-follow", groupId = "notification-follow-group")
    public void consumeFollowNotification(NotificationEvent event, Acknowledgment ack) {
        try {
            logger.info("Received follow notification: {}", event);
            // 팔로우 알림에 특화된 처리 로직 추가 가능
            notificationService.saveNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing follow notification: {}", e.getMessage(), e);
            notificationProducer.retryNotification(event, 3);
        }
    }
    
    /**
     * 일괄 처리 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "notification-batch", groupId = "notification-batch-group")
    public void consumeBatch(List<NotificationEvent> events, Acknowledgment ack) {
        try {
            logger.info("Received batch notification with {} events", events.size());
            CompletableFuture<Void> future = notificationService.saveNotificationBatch(events);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    ack.acknowledge();
                    logger.info("Processed batch notification with {} events", events.size());
                } else {
                    logger.error("Error processing batch notification: {}", ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            logger.error("Error processing batch notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 재시도 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "notification-retry", groupId = "notification-retry-group")
    public void consumeRetry(NotificationEvent event, Acknowledgment ack) {
        try {
            logger.info("Processing retry notification: {}", event);
            notificationService.saveNotification(event);
            ack.acknowledge();
            logger.info("Successfully processed retry notification: {}", event);
        } catch (Exception e) {
            logger.error("Error processing retry notification: {}", e.getMessage(), e);
            // 재시도 실패 시 일정 시간 대기 후 다시 처리
            try {
                Thread.sleep(5000); // 5초 대기
                notificationService.saveNotification(event);
                ack.acknowledge();
            } catch (Exception ex) {
                logger.error("Final retry failed for notification: {}", event, ex);
                // 최종 실패 시 DLQ(Dead Letter Queue)로 이동하거나 DB에 실패 기록 남김
            }
        }
    }
}