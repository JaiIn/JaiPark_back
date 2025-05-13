package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Post;
import org.example.jaipark_back.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    
    // 키셋 페이지네이션을 위한 메서드
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user WHERE p.id < :lastPostId ORDER BY p.id DESC LIMIT :limit")
    List<Post> findPostsBeforeId(@Param("lastPostId") Long lastPostId, @Param("limit") int limit);
    
    // 초기 페이지 로드를 위한 메서드
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user ORDER BY p.id DESC LIMIT :limit")
    List<Post> findFirstPage(@Param("limit") int limit);
    
    // 시간 기반 키셋 페이지네이션
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user WHERE (p.createdAt < :createdAt) OR (p.createdAt = :createdAt AND p.id < :id) ORDER BY p.createdAt DESC, p.id DESC LIMIT :limit")
    List<Post> findPostsBeforeTimeAndId(
        @Param("createdAt") LocalDateTime createdAt, 
        @Param("id") Long id, 
        @Param("limit") int limit);
    
    // 팔로우한 사용자의 게시물에 대한 키셋 페이지네이션
    @Query("SELECT p FROM Post p WHERE p.user IN :users AND ((p.createdAt < :createdAt) OR (p.createdAt = :createdAt AND p.id < :id)) ORDER BY p.createdAt DESC, p.id DESC LIMIT :limit")
    List<Post> findPostsByUsersBeforeTimeAndId(
        @Param("users") List<User> users,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("id") Long id,
        @Param("limit") int limit);
}