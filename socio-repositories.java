package com.socio.repository;

import com.socio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.email LIKE '%@socio.com'")
    List<User> findAllAdmins();
    
    List<User> findByDateOfBirth(LocalDate date);
}

package com.socio.repository;

import com.socio.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);
    
    @Query("SELECT p FROM Post p WHERE p.userId = ?1 OR p.userId IN (SELECT f.followeeId FROM Follow f WHERE f.followerId = ?1)")
    Page<Post> findFeedForUser(Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.userId IN (SELECT gm.userId FROM GroupMember gm WHERE gm.groupId = ?1)")
    List<Post> findByGroupId(Long groupId);
}

package com.socio.repository;

import com.socio.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);
    
    long countByPostId(Long postId);
}

package com.socio.repository;

import com.socio.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
    
    long countByPostId(Long postId);
}

package com.socio.repository;

import com.socio.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    
    List<Follow> findByFollowerId(Long followerId);
    
    List<Follow> findByFolloweeId(Long followeeId);
    
    long countByFolloweeId(Long followeeId);
}

package com.socio.repository;

import com.socio.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByCreatorId(Long creatorId);
    
    @Query("SELECT g FROM Group g WHERE g.creatorId = ?1 OR g.id IN (SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = ?1)")
    List<Group> findGroupsForUser(Long userId);
}

package com.socio.repository;

import com.socio.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);
    
    List<GroupMember> findByUserId(Long userId);
    
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
}

package com.socio.repository;

import com.socio.entity.Report;
import com.socio.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(ReportStatus status);
    
    List<Report> findByPostId(Long postId);
}
