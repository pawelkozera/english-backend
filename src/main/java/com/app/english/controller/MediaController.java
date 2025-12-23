package com.app.english.controller;

import com.app.english.dto.media.MediaInfoResponse;
import com.app.english.dto.media.MediaUploadResponse;
import com.app.english.service.MediaService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // Uploads a private media file (JWT required). Returns media id + URL.
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse upload(@RequestPart("file") MultipartFile file, Authentication auth) {
        return mediaService.upload(auth.getName(), file);
    }

    // Metadata (JWT required).
    @GetMapping("/{id}/info")
    public MediaInfoResponse info(@PathVariable @NotNull Long id, Authentication auth) {
        return mediaService.info(auth.getName(), id);
    }

    // Serves the file (JWT required).
    @GetMapping("/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> get(@PathVariable @NotNull Long id, Authentication auth) {
        var loaded = mediaService.load(auth.getName(), id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(loaded.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFilename(loaded.originalName()) + "\"")
                .contentLength(loaded.size())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(loaded.resource());
    }

    // Deletes the file (owner only).
    @DeleteMapping("/{id}")
    public void delete(@PathVariable @NotNull Long id, Authentication auth) {
        mediaService.delete(auth.getName(), id);
    }

    private String safeFilename(String s) {
        if (s == null) return "file";
        return s.replaceAll("[\\r\\n\"]", "_");
    }
}
