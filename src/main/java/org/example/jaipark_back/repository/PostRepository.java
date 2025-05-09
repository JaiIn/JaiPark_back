package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Post;
import org.example.jaipark_back.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByUserOrderByCreatedAtDesc(User user);

    List<Post> findAllByUserInOrderByCreatedAtDesc(List<User> users);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.comments c LEFT JOIN FETCH c.user WHERE p.id = :id")
    Optional<Post> findByIdWithUserAndComments(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.comments c LEFT JOIN FETCH c.user ORDER BY p.createdAt DESC")
    Page<Post> findAllWithUserAndComments(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<Post> searchByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);
}