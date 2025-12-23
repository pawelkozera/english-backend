package com.app.english.dto.tasks;

import com.app.english.models.TaskStatus;
import com.app.english.models.TaskType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TaskResponse(
        Long id,
        String title,
        TaskType type,
        TaskStatus status,
        Map<String, Object> payload,
        List<Long> vocabularyIds,
        Instant createdAt,
        Instant updatedAt
) {}
