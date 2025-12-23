package com.app.english.dto.vocabulary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVocabularyRequest(
        @NotBlank @Size(max = 200) String termEn,
        @NotBlank @Size(max = 200) String termPl,
        @Size(max = 1000) String exampleEn,
        @Size(max = 1000) String examplePl,
        Long imageMediaId,
        Long audioMediaId
) {}
