package org.example.jaipark_back.service;

import org.example.jaipark_back.dto.CommentResponse;
import org.example.jaipark_back.dto.PostRequest;
import org.example.jaipark_back.dto.PostResponse;
import org.example.jaipark_back.entity.Post;
import org.example.jaipark_back.entity.User;
import org.example.jaipark_back.entity.Like;
import org.example.jaipark_back.entity.Bookmark;
import org.example.jaipark_back.repository.PostRepository;
import org.example.jaipark_back.repository.UserRepository;
import org.example.jaipark_back.repository.LikeRepository;
import org.example.jaipark_back.repository.BookmarkRepository;
import org.example.jaipark_back.repository.FollowRepository;
import org.example.jaipark_back.exception.PostException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

import java.util.List;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private FollowRepository followRepository;

    @Transactional
    public PostResponse createPost(@Valid PostRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new PostException("사용자를 찾을 수 없습니다."));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUser(user);

        Post savedPost = postRepository.save(post);
        return convertToResponse(savedPost);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAllWithUserAndComments(pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long id) {
        Post post = postRepository.findByIdWithUserAndComments(id)
                .orElseThrow(PostException.PostNotFoundException::new);
        return convertToResponse(post);
    }

    @Transactional
    public PostResponse updatePost(Long id, @Valid PostRequest request, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(PostException.PostNotFoundException::new);

        if (!post.getUser().getUsername().equals(username)) {
            throw new PostException.UnauthorizedException();
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        Post updatedPost = postRepository.save(post);
        return convertToResponse(updatedPost);
    }

    @Transactional
    public void deletePost(Long id, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(PostException.PostNotFoundException::new);

        if (!post.getUser().getUsername().equals(username)) {
            throw new PostException.UnauthorizedException();
        }

        postRepository.delete(post);
    }

    @Transactional
    public boolean toggleLike(Long postId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        if (likeRepository.existsByUserAndPost(user, post)) {
            likeRepository.deleteByUserAndPost(user, post);
            return false;
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setPost(post);
            likeRepository.save(like);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public long countLikes(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return likeRepository.countByPost(post);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long postId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        return likeRepository.existsByUserAndPost(user, post);
    }

    @Transactional
    public boolean toggleBookmark(Long postId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            bookmarkRepository.deleteByUserAndPost(user, post);
            return false;
        } else {
            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setPost(post);
            bookmarkRepository.save(bookmark);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public long countBookmarks(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return bookmarkRepository.countByPost(post);
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long postId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        return bookmarkRepository.existsByUserAndPost(user, post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return postRepository.findAllByUserOrderByCreatedAtDesc(user).stream().map(this::convertToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getLikedPosts(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return likeRepository.findAllByUser(user).stream().map(like -> convertToResponse(like.getPost())).toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getBookmarkedPosts(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return bookmarkRepository.findAllByUser(user).stream().map(bookmark -> convertToResponse(bookmark.getPost())).toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getFollowingsPosts(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        var followings = followRepository.findByFollower(user);
        var followingUsers = followings.stream().map(f -> f.getFollowing()).toList();
        return postRepository.findAllByUserInOrderByCreatedAtDesc(followingUsers).stream().map(this::convertToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> searchPosts(String keyword, int page, int size) {
        Page<Post> posts = postRepository.searchByTitleOrContent(keyword, PageRequest.of(page, size));
        return posts.map(this::convertToResponse);
    }

    private PostResponse convertToResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setUsername(post.getUser().getUsername());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        response.setComments(post.getComments().stream()
                .map(comment -> {
                    CommentResponse commentResponse = new CommentResponse();
                    commentResponse.setId(comment.getId());
                    commentResponse.setContent(comment.getContent());
                    commentResponse.setUsername(comment.getUser().getUsername());
                    commentResponse.setCreatedAt(comment.getCreatedAt());
                    commentResponse.setUpdatedAt(comment.getUpdatedAt());
                    return commentResponse;
                })
                .collect(java.util.stream.Collectors.toList()));
        return response;
    }
} 