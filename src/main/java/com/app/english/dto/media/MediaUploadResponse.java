package com.app.english.dto.media;

import com.app.english.models.MediaType;

import java.time.Instant;

public record MediaUploadResponse(
        Long id,
        MediaType type,
        String url,
        String contentType,
        long size,
        Instant createdAt
) {}