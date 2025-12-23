package com.app.english.dto.tasks;

import com.app.english.models.TaskStatus;
import com.app.english.models.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull TaskType type,
        TaskStatus status,
        Map<String, Object> payload,
        // Optional: for vocab-based tasks
        List<Long> vocabularyIds
) {}
