package com.app.english.dto.lessons;

import com.app.english.models.LessonItemType;

public record LessonItemResponse(
        int position,
        LessonItemType itemType,
        Long taskId
) {}
