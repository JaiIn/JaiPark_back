package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // 채팅방 ID로 메시지 목록 조회 (페이징)
    Page<ChatMessage> findByChatRoomIdOrderByTimestampDesc(String chatRoomId, Pageable pageable);
    
    // 채팅방 ID로 최근 메시지 조회 (제한된 개수)
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoomId = :chatRoomId ORDER BY m.timestamp DESC")
    List<ChatMessage> findRecentMessagesByChatRoomId(@Param("chatRoomId") String chatRoomId, Pageable pageable);
    
    // 채팅방 ID로 가장 최근 메시지 1개 조회
    ChatMessage findTopByChatRoomIdOrderByTimestampDesc(String chatRoomId);
    
    // 유저가 받은 메시지 중 안 읽은 메시지 개수 조회
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiverId = :userId AND m.isRead = false")
    int countUnreadMessagesByReceiverId(@Param("userId") String userId);
    
    // 특정 채팅방에서 유저가 받은 메시지 중 안 읽은 메시지 개수 조회
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoomId = :chatRoomId AND m.receiverId = :userId AND m.isRead = false")
    int countUnreadMessagesInChatRoom(@Param("chatRoomId") String chatRoomId, @Param("userId") String userId);
    
    // 특정 채팅방의 메시지 모두 읽음으로 표시
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatRoomId = :chatRoomId AND m.receiverId = :userId AND m.isRead = false")
    void markAllAsReadInChatRoom(@Param("chatRoomId") String chatRoomId, @Param("userId") String userId);
    
    // 유저가 참여한 채팅방 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT m.chatRoomId FROM ChatMessage m WHERE m.senderId = :userId OR m.receiverId = :userId")
    List<String> findChatRoomIdsByUserId(@Param("userId") String userId);
}
