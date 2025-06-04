package com.example.signupauth.repository;

import com.example.signupauth.model.Bug;
import com.example.signupauth.model.Bug.Priority;
import com.example.signupauth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugRepository extends JpaRepository<Bug, Long> {
    @Query("SELECT b FROM Bug b WHERE b.assignedUser = :user ORDER BY b.priority DESC")
    List<Bug> findByAssignedUserOrderByPriorityDesc(@Param("user") User assignedUser);

    @Query("SELECT b FROM Bug b WHERE b.assignedUser = :user AND b.resolved = :resolved")
    List<Bug> findByAssignedUserAndResolved(@Param("user") User assignedUser, @Param("resolved") boolean resolved);

    @Query("SELECT b FROM Bug b WHERE b.assignedUser = :user AND b.priority = :priority")
    List<Bug> findByAssignedUserAndPriority(@Param("user") User assignedUser, @Param("priority") Priority priority);

    @Query("SELECT b FROM Bug b WHERE b.assignedUser = :user AND LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Bug> findByAssignedUserAndTitleContainingIgnoreCase(@Param("user") User assignedUser, @Param("title") String title);

    @Query("SELECT b FROM Bug b WHERE b.reporterUser = :user ORDER BY b.priority DESC")
    List<Bug> findByReporterUserOrderByPriorityDesc(@Param("user") User reporterUser);

    @Query("SELECT b FROM Bug b WHERE b.reporterUser = :user AND b.resolved = :resolved")
    List<Bug> findByReporterUserAndResolved(@Param("user") User reporterUser, @Param("resolved") boolean resolved);

    @Query("SELECT b FROM Bug b WHERE b.reporterUser = :user AND b.priority = :priority")
    List<Bug> findByReporterUserAndPriority(@Param("user") User reporterUser, @Param("priority") Priority priority);

    @Query("SELECT b FROM Bug b WHERE b.reporterUser = :user AND LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Bug> findByReporterUserAndTitleContainingIgnoreCase(@Param("user") User reporterUser, @Param("title") String title);
} 