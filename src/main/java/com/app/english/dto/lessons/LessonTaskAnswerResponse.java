package com.app.english.dto.lessons;

import com.app.english.models.LessonAnswerStatus;

import java.time.Instant;

public record LessonTaskAnswerResponse(
        Long assignmentId,
        Long taskId,
        LessonAnswerStatus status,
        Object answer,
        Instant updatedAt,
        Instant submittedAt
) {}

