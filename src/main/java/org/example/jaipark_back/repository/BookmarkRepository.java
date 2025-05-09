package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Bookmark;
import org.example.jaipark_back.entity.Post;
import org.example.jaipark_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserAndPost(User user, Post post);
    long countByPost(Post post);
    void deleteByUserAndPost(User user, Post post);
    boolean existsByUserAndPost(User user, Post post);
    List<Bookmark> findAllByUser(User user);
} 