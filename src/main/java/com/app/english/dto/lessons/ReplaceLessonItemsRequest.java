package com.app.english.dto.lessons;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceLessonItemsRequest(
        @NotNull List<Long> taskIds
) {}
