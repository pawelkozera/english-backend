package com.app.english.dto.tasks;

import com.app.english.models.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull TaskStatus status,
        Map<String, Object> payload
) {}
