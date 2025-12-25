package com.app.english.dto.lessons;


import java.time.Instant;

public record AssignLessonRequest(
        Long assignedToUserId,   // null => whole group
        Instant visibleFrom,     // null => immediately visible
        Instant visibleTo        // null => no end date
) {}

