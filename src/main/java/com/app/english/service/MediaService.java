package com.app.english.service;

import com.app.english.config.MediaProperties;
import com.app.english.dto.media.MediaInfoResponse;
import com.app.english.dto.media.MediaUploadResponse;
import com.app.english.exceptions.ForbiddenException;
import com.app.english.exceptions.MediaInUseException;
import com.app.english.exceptions.MediaNotFoundException;
import com.app.english.models.MediaFile;
import com.app.english.models.MediaType;
import com.app.english.models.User;
import com.app.english.repository.MediaFileRepository;
import com.app.english.repository.UserRepository;
import com.app.english.repository.VocabularyRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class MediaService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Set<String> AUDIO_TYPES = Set.of("audio/mpeg", "audio/wav", "audio/ogg");

    private final MediaFileRepository mediaRepo;
    private final UserRepository userRepo;
    private final MediaStorageService storage;
    private final MediaProperties props;
    private final VocabularyRepository vocabularyRepository;

    public MediaService(MediaFileRepository mediaRepo, UserRepository userRepo, MediaStorageService storage, MediaProperties props, VocabularyRepository vocabularyRepository) {
        this.mediaRepo = mediaRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        this.props = props;
        this.vocabularyRepository = vocabularyRepository;
    }

    @Transactional
    public MediaUploadResponse upload(String actorEmail, MultipartFile file) {
        User actor = userRepo.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String contentType = normalizeContentType(file.getContentType());
        MediaType type = detectType(contentType);

        long size = file.getSize();
        if (type == MediaType.IMAGE && size > props.maxImageBytes()) {
            throw new IllegalArgumentException("Image too large");
        }
        if (type == MediaType.AUDIO && size > props.maxAudioBytes()) {
            throw new IllegalArgumentException("Audio too large");
        }

        String storageKey = storage.generateStorageKey(type, file.getOriginalFilename(), contentType);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read uploaded file", e);
        }

        storage.save(storageKey, bytes);

        MediaFile saved = mediaRepo.save(new MediaFile(
                type,
                storageKey,
                (file.getOriginalFilename() == null) ? "file" : file.getOriginalFilename(),
                contentType,
                size,
                actor
        ));

        return new MediaUploadResponse(
                saved.getId(),
                saved.getType(),
                "/api/media/" + saved.getId(),
                saved.getContentType(),
                saved.getSize(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public MediaInfoResponse info(String actorEmail, Long id) {
        // auth is required by security config; this is "private" (not public internet)
        MediaFile mf = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        return new MediaInfoResponse(
                mf.getId(),
                mf.getType(),
                mf.getOriginalName(),
                mf.getContentType(),
                mf.getSize(),
                mf.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public LoadedMedia load(String actorEmail, Long id) {
        MediaFile mf = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        // why: MVP allows any authenticated user (students need to access teacher media in tasks).
        // TODO: tighten with assignment/group checks when lessons/tasks assignment exists.

        Resource res = storage.loadAsResource(mf.getStorageKey());
        if (!res.exists()) {
            throw new MediaNotFoundException("Media file missing on disk");
        }

        return new LoadedMedia(res, mf.getContentType(), mf.getOriginalName(), mf.getSize());
    }

    @Transactional
    public void delete(String actorEmail, Long id) {
        User actor = userRepo.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        MediaFile mf = mediaRepo.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        if (!mf.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        // Block deletion when referenced by vocabulary.
        if (vocabularyRepository.existsByImageMediaId(id) || vocabularyRepository.existsByAudioMediaId(id)) {
            throw new MediaInUseException("Media is in use");
        }

        mediaRepo.delete(mf);
        storage.deleteIfExists(mf.getStorageKey());
    }

    public record LoadedMedia(Resource resource, String contentType, String originalName, long size) {}

    private String normalizeContentType(String ct) {
        if (ct == null) return "";
        return ct.toLowerCase(Locale.ROOT).trim();
    }

    private MediaType detectType(String contentType) {
        if (IMAGE_TYPES.contains(contentType)) return MediaType.IMAGE;
        if (AUDIO_TYPES.contains(contentType)) return MediaType.AUDIO;
        throw new IllegalArgumentException("Unsupported content-type: " + contentType);
    }
}
