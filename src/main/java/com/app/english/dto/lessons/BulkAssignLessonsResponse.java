package com.app.english.dto.lessons;

import java.util.List;

public record BulkAssignLessonsResponse(
        List<LessonAssignmentResponse> created,
        List<Skipped> skipped
) {
    public record Skipped(Long lessonId, String reason, Long existingAssignmentId) {}
}
