package com.app.english.dto.vocabulary;

import java.time.Instant;

public record VocabularyResponse(
        Long id,
        String termEn,
        String termPl,
        String exampleEn,
        String examplePl,
        Long imageMediaId,
        Long audioMediaId,
        Instant createdAt,
        Instant updatedAt
) {}
