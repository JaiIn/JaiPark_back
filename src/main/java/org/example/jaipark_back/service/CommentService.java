package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.CommentRequest;
import org.example.jaipark_back.dto.CommentResponse;
import org.example.jaipark_back.dto.NotificationEvent;
import org.example.jaipark_back.entity.Comment;
import org.example.jaipark_back.entity.Post;
import org.example.jaipark_back.entity.User;
import org.example.jaipark_back.repository.CommentRepository;
import org.example.jaipark_back.repository.PostRepository;
import org.example.jaipark_back.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationProducer notificationProducer;

    /**
     * 댓글 생성 메서드 - 비동기 처리를 통해 메인 작업과 알림 전송을 분리
     */
    @Transactional
    public CommentResponse createComment(Long postId, CommentRequest request, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);
        
        // 비동기로 알림 처리
        sendCommentNotificationAsync(savedComment, post, user);

        return convertToResponse(savedComment);
    }
    
    /**
     * 댓글 알림을 비동기적으로 처리하는 메서드
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> sendCommentNotificationAsync(Comment comment, Post post, User commentAuthor) {
        try {
            // 게시글 작성자에게 알림 (본인이 자기 글에 댓글 달면 알림 X)
            if (!post.getUser().getUsername().equals(commentAuthor.getUsername())) {
                NotificationEvent event = new NotificationEvent();
                event.setUsername(post.getUser().getUsername());
                event.setType("COMMENT");
                event.setMessage(commentAuthor.getNickname() + "님이 회원님의 글에 댓글을 남겼습니다: " + comment.getContent());
                event.setPostId(post.getId());
                
                // 알림을 타입별 토픽으로 전송
                notificationProducer.sendTypedNotification(event);
                logger.info("Comment notification sent to {}", post.getUser().getUsername());
            }
            
            // 특정 포스트에 다른 사람들이 댓글을 달았다면, 그 댓글에 참여한 모든 사람에게도 알림
            // (본인 제외, 중복 제외)
            sendNotificationsToCommentParticipants(comment, post, commentAuthor);
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error sending comment notification: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 댓글 참여자들에게 알림을 보내는 메서드
     */
    private void sendNotificationsToCommentParticipants(Comment newComment, Post post, User commentAuthor) {
        // 해당 게시물의 모든 댓글 작성자를 찾음 (중복 제거, 본인과 게시글 작성자 제외)
        List<User> commentParticipants = commentRepository.findByPostId(post.getId()).stream()
            .map(Comment::getUser)
            .filter(user -> !user.getUsername().equals(commentAuthor.getUsername())) // 자기 자신 제외
            .filter(user -> !user.getUsername().equals(post.getUser().getUsername())) // 게시글 작성자 제외 (이미 위에서 알림 보냄)
            .distinct()
            .collect(Collectors.toList());
        
        if (commentParticipants.isEmpty()) {
            return;
        }
        
        // 배치 알림 처리를 위한 이벤트 리스트
        List<NotificationEvent> events = new ArrayList<>();
        
        for (User participant : commentParticipants) {
            NotificationEvent event = new NotificationEvent();
            event.setUsername(participant.getUsername());
            event.setType("COMMENT_REPLY");
            event.setMessage(commentAuthor.getNickname() + "님이 회원님이 댓글 단 게시글에 새 댓글을 남겼습니다.");
            event.setPostId(post.getId());
            events.add(event);
        }
        
        // 배치로 알림 전송
        if (!events.isEmpty()) {
            notificationProducer.sendBatchNotifications("comment-participants", events);
            logger.info("Batch notifications sent to {} comment participants", events.size());
        }
    }

    /**
     * 게시물 ID로 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(Long id, CommentRequest request, String username) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized to update this comment");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);
        return convertToResponse(updatedComment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long id, String username) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    /**
     * 사용자가 작성한 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return commentRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 배치로 여러 댓글을 가져오는 최적화된 메서드
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsBatch(Long postId, int limit, Long lastCommentId) {
        List<Comment> comments;
        if (lastCommentId == null) {
            comments = commentRepository.findTopCommentsByPostId(postId, limit);
        } else {
            comments = commentRepository.findCommentsByPostIdWithCursor(postId, lastCommentId, limit);
        }
        
        return comments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 엔티티를 DTO로 변환
     */
    private CommentResponse convertToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUsername(comment.getUser().getUsername());
        response.setNickname(comment.getUser().getNickname());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setPostId(comment.getPost().getId());
        response.setPostTitle(comment.getPost().getTitle());
        return response;
    }
}