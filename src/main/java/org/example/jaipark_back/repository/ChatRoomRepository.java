package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    
    // 유저가 참여한 모든 채팅방 조회
    @Query("SELECT r FROM ChatRoom r WHERE r.user1Id = :userId OR r.user2Id = :userId ORDER BY r.lastMessageTime DESC")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") String userId);
    
    // 두 유저 간의 채팅방 조회
    @Query("SELECT r FROM ChatRoom r WHERE (r.user1Id = :user1Id AND r.user2Id = :user2Id) OR (r.user1Id = :user2Id AND r.user2Id = :user1Id)")
    Optional<ChatRoom> findChatRoomByUsers(@Param("user1Id") String user1Id, @Param("user2Id") String user2Id);
    
    // 채팅방 ID로 채팅방 조회
    Optional<ChatRoom> findById(String chatRoomId);
}
