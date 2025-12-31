package com.app.english.repository;

import com.app.english.models.LessonTaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface LessonTaskProgressRepository extends JpaRepository<LessonTaskProgress, Long> {

    Optional<LessonTaskProgress> findByProgressIdAndTaskId(Long progressId, Long taskId);

    @Query("""
        select ltp.task.id
        from LessonTaskProgress ltp
        where ltp.progress.id = :progressId and ltp.completed = true
    """)
    Set<Long> findCompletedTaskIds(Long progressId);
}
