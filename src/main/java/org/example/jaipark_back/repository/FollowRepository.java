package org.example.jaipark_back.repository;

import org.example.jaipark_back.entity.Follow;
import org.example.jaipark_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerAndFollowing(User follower, User following);
    void deleteByFollowerAndFollowing(User follower, User following);
    List<Follow> findByFollower(User follower);
    List<Follow> findByFollowing(User following);
    long countByFollower(User follower);
    long countByFollowing(User following);
} 