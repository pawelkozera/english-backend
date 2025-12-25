package com.app.english.repository;

import com.app.english.models.Lesson;
import com.app.english.models.LessonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Page<Lesson> findByCreatedByIdAndStatusInAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            Long ownerId,
            List<LessonStatus> statuses,
            String q,
            Pageable pageable
    );

    Page<Lesson> findByCreatedByIdAndStatusInOrderByUpdatedAtDesc(
            Long ownerId,
            List<LessonStatus> statuses,
            Pageable pageable
    );
}
