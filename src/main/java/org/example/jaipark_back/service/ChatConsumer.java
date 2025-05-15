package org.example.jaipark_back.service;

import lombok.RequiredArgsConstructor;
import org.example.jaipark_back.dto.ChatEvent;
import org.example.jaipark_back.dto.ChatMessageDto;
import org.example.jaipark_back.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ChatConsumer.class);
    
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 채팅 메시지 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "chat-message", groupId = "chat-group")
    public void consumeChatMessage(ChatEvent event, Acknowledgment ack) {
        try {
            logger.info("Received chat message event: {}", event);
            
            // 이벤트에서 메시지 추출 - LinkedHashMap을 ChatMessageDto로 변환
            var data = event.getData();
            ChatMessageDto messageDto;
            
            if (data instanceof LinkedHashMap) {
                // JSON 역직렬화 과정에서 LinkedHashMap으로 변환된 경우
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) data;
                
                messageDto = new ChatMessageDto();
                messageDto.setId(map.containsKey("id") ? ((Number) map.get("id")).longValue() : null);
                messageDto.setSenderId((String) map.get("senderId"));
                messageDto.setReceiverId((String) map.get("receiverId"));
                messageDto.setContent((String) map.get("content"));
                messageDto.setChatRoomId((String) map.get("chatRoomId"));
                
                // timestamp 처리
                if (map.containsKey("timestamp")) {
                    Object timestamp = map.get("timestamp");
                    if (timestamp instanceof String) {
                        messageDto.setTimestamp(LocalDateTime.parse((String) timestamp));
                    } else if (timestamp instanceof LinkedHashMap) {
                        // 아무것도 하지 않음
                        messageDto.setTimestamp(LocalDateTime.now());
                    } else if (timestamp instanceof List) {
                        // [2025, 5, 15, 11, 32, 51, 957000000] 형태
                        List<?> timestampList = (List<?>) timestamp;
                        if (timestampList.size() >= 7) {
                            try {
                                int year = ((Number) timestampList.get(0)).intValue();
                                int month = ((Number) timestampList.get(1)).intValue();
                                int day = ((Number) timestampList.get(2)).intValue();
                                int hour = ((Number) timestampList.get(3)).intValue();
                                int minute = ((Number) timestampList.get(4)).intValue();
                                int second = ((Number) timestampList.get(5)).intValue();
                                int nano = ((Number) timestampList.get(6)).intValue();
                                
                                messageDto.setTimestamp(LocalDateTime.of(year, month, day, hour, minute, second, nano));
                            } catch (Exception e) {
                                logger.error("Error parsing timestamp list: {}", e.getMessage());
                                messageDto.setTimestamp(LocalDateTime.now());
                            }
                        } else {
                            messageDto.setTimestamp(LocalDateTime.now());
                        }
                    } else {
                        messageDto.setTimestamp(LocalDateTime.now());
                    }
                } else {
                    messageDto.setTimestamp(LocalDateTime.now());
                }
                
                // read 처리
                if (map.containsKey("read")) {
                    messageDto.setRead((Boolean) map.get("read"));
                } else {
                    messageDto.setRead(false);
                }
                
                // type 처리
                if (map.containsKey("type")) {
                    String typeStr = (String) map.get("type");
                    try {
                        messageDto.setType(ChatMessage.MessageType.valueOf(typeStr));
                    } catch (IllegalArgumentException e) {
                        messageDto.setType(ChatMessage.MessageType.TEXT); // 기본값
                    }
                } else {
                    messageDto.setType(ChatMessage.MessageType.TEXT); // 기본값
                }
                
            } else if (data instanceof ChatMessageDto) {
                // 이미 ChatMessageDto 객체인 경우
                messageDto = (ChatMessageDto) data;
            } else {
                logger.error("Unknown data type: {}", data.getClass().getName());
                ack.acknowledge(); // 오류가 있어도 확인하여 다음 메시지 처리
                return;
            }
            
            // WebSocket을 통해 사용자에게 메시지 전송
            String destination = "/topic/chat/" + event.getReceiverId();
            messagingTemplate.convertAndSend(destination, messageDto);
            
            ack.acknowledge();
            logger.info("Processed chat message event: {}", event);
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            // 오류 발생 시 메시지 재처리 로직 구현 가능
        }
    }
    
    /**
     * 읽음 확인 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "chat-read", groupId = "chat-read-group")
    public void consumeReadReceipt(ChatEvent event, Acknowledgment ack) {
        try {
            logger.info("Received read receipt event: {}", event);
            
            // WebSocket을 통해 사용자에게 읽음 상태 전송
            String destination = "/topic/chat/read/" + event.getReceiverId();
            messagingTemplate.convertAndSend(destination, event);
            
            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing read receipt: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 타이핑 상태 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "chat-typing", groupId = "chat-typing-group")
    public void consumeTypingStatus(ChatEvent event, Acknowledgment ack) {
        try {
            logger.info("Received typing status event: {}", event);
            
            // WebSocket을 통해 사용자에게 타이핑 상태 전송
            String destination = "/topic/chat/typing/" + event.getReceiverId();
            messagingTemplate.convertAndSend(destination, event);
            
            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing typing status: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 온라인 상태 토픽의 메시지를 소비합니다.
     */
    @KafkaListener(topics = "chat-status", groupId = "chat-status-group")
    public void consumeOnlineStatus(ChatEvent event, Acknowledgment ack) {
        try {
            logger.info("Received online status event: {}", event);
            
            // WebSocket을 통해 모든 사용자에게 온라인 상태 전송
            String destination = "/topic/chat/status";
            messagingTemplate.convertAndSend(destination, event);
            
            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing online status: {}", e.getMessage(), e);
        }
    }
}
