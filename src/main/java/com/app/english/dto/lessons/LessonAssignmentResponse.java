package com.app.english.dto.lessons;

import com.app.english.models.LessonStatus;

import java.time.Instant;

public record LessonAssignmentResponse(
        Long id,
        Long groupId,
        Long lessonId,
        String lessonTitle,
        LessonStatus lessonStatus,
        Long assignedToUserId, // null => group-wide
        long displayOrder,
        Instant visibleFrom,
        Instant visibleTo,
        Instant createdAt
) {}
