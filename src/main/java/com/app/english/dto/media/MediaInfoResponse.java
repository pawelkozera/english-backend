package com.app.english.dto.media;

import com.app.english.models.MediaType;

import java.time.Instant;

public record MediaInfoResponse(
        Long id,
        MediaType type,
        String originalName,
        String contentType,
        long size,
        Instant createdAt
) {}
