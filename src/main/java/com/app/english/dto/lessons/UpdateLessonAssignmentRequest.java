package com.app.english.dto.lessons;

import java.time.Instant;

public record UpdateLessonAssignmentRequest(
        Instant visibleFrom,
        Instant visibleTo
) {}
