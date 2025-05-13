package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Comment;
import org.example.jaipark_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    /**
     * 게시물 ID로 댓글 목록을 최신순으로 조회
     */
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
    
    /**
     * 게시물 ID로 댓글 목록 조회 (Fetch Join으로 성능 최적화)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    List<Comment> findByPostIdWithUser(@Param("postId") Long postId);
    
    /**
     * 사용자가 작성한 댓글 목록을 최신순으로 조회
     */
    List<Comment> findAllByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 게시물 ID로 댓글 목록을 조회 (키셋 페이지네이션 적용)
     */
    @Query(value = "SELECT c.* FROM comments c WHERE c.post_id = :postId AND c.id < :lastCommentId ORDER BY c.id DESC LIMIT :limit", nativeQuery = true)
    List<Comment> findCommentsByPostIdWithCursor(
            @Param("postId") Long postId,
            @Param("lastCommentId") Long lastCommentId,
            @Param("limit") int limit);
    
    /**
     * 게시물 ID로 최신 댓글 조회
     */
    @Query(value = "SELECT c.* FROM comments c WHERE c.post_id = :postId ORDER BY c.id DESC LIMIT :limit", nativeQuery = true)
    List<Comment> findTopCommentsByPostId(
            @Param("postId") Long postId,
            @Param("limit") int limit);
    
    /**
     * 게시물 ID로 댓글 목록 조회 (중간 쿼리 최적화)
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    List<Comment> findByPostId(@Param("postId") Long postId);
    
    /**
     * 게시물 ID로 댓글 개수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
    
    /**
     * 사용자 ID와 게시물 ID로 댓글 개수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.user.id = :userId")
    long countByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}