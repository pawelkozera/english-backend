package com.app.english.dto.lessons;

import java.time.Instant;
import java.util.List;

public record BulkAssignLessonsRequest(
        List<Long> lessonIds,
        Long assignedToUserId,
        Instant visibleFrom,
        Instant visibleTo
) {}
