package com.app.english.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        String uploadDir,
        long maxImageBytes,
        long maxAudioBytes
) {}
