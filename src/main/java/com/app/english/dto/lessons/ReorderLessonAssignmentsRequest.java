package com.app.english.dto.lessons;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderLessonAssignmentsRequest(
        Long userId, // null => reorder group-wide; not-null => reorder per-user bucket
        @NotNull List<Long> assignmentIds
) {}
