package com.app.english.repository;

import com.app.english.models.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByUserIdAndAssignmentId(Long userId, Long assignmentId);
}
