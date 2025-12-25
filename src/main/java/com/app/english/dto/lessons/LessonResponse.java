package com.app.english.dto.lessons;

import com.app.english.models.LessonStatus;

import java.time.Instant;
import java.util.List;

public record LessonResponse(
        Long id,
        String title,
        String description,
        LessonStatus status,
        List<LessonItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}
