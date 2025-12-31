package com.app.english.repository;

import com.app.english.models.LessonTaskAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonTaskAnswerRepository extends JpaRepository<LessonTaskAnswer, Long> {
    Optional<LessonTaskAnswer> findByProgressIdAndTaskId(Long progressId, Long taskId);
}
