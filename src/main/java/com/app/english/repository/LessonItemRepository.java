package com.app.english.repository;

import com.app.english.models.LessonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LessonItemRepository extends JpaRepository<LessonItem, Long> {
    boolean existsByTaskId(Long taskId);

    @Query("""
        select li from LessonItem li
        where li.lesson.id = :lessonId
        order by li.position asc
    """)
    List<LessonItem> findByLessonIdOrderByPosition(Long lessonId);

    @Modifying
    @Query("delete from LessonItem li where li.lesson.id = :lessonId")
    int deleteByLessonId(Long lessonId);
}

