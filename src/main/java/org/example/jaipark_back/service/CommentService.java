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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationProducer notificationProducer;

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

        // 게시글 작성자에게 알림 (본인이 자기 글에 댓글 달면 알림 X)
        if (!post.getUser().getUsername().equals(username)) {
            NotificationEvent event = new NotificationEvent();
            event.setUsername(post.getUser().getUsername());
            event.setType("COMMENT");
            event.setMessage(user.getNickname() + "님이 회원님의 글에 댓글을 남겼습니다: " + comment.getContent());
            event.setPostId(post.getId());
            notificationProducer.sendNotification(event);
        }

        return convertToResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

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

    @Transactional
    public void deleteComment(Long id, String username) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return commentRepository.findAllByUserOrderByCreatedAtDesc(user).stream().map(this::convertToResponse).collect(Collectors.toList());
    }

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