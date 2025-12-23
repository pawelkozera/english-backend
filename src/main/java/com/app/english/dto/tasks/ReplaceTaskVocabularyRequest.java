package com.app.english.dto.tasks;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceTaskVocabularyRequest(
        @NotNull List<Long> vocabularyIds
) {}
