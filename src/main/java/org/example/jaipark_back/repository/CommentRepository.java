package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Comment;
import org.example.jaipark_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
    List<Comment> findAllByUserOrderByCreatedAtDesc(User user);
} 