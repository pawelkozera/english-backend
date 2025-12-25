package com.app.english.dto.lessons;

import com.app.english.models.LessonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLessonRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LessonStatus status
) {}
