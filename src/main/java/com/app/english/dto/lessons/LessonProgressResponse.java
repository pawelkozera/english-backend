package com.app.english.dto.lessons;

import com.app.english.models.LessonProgressStatus;

import java.time.Instant;
import java.util.Set;

public record LessonProgressResponse(
        Long assignmentId,
        Long lessonId,
        LessonProgressStatus status,
        Instant startedAt,
        Instant completedAt,
        Set<Long> completedTaskIds,
        int doneCount,
        int totalCount
) {}
