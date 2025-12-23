package com.app.english.service;

import com.app.english.config.MediaProperties;
import com.app.english.models.MediaType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class MediaStorageService {

    private final Path baseDir;

    public MediaStorageService(MediaProperties props) {
        this.baseDir = Paths.get(props.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload dir: " + this.baseDir, e);
        }
    }

    public String generateStorageKey(MediaType type, String originalFilename, String contentType) {
        String ext = chooseExtension(originalFilename, contentType);
        String folder = (type == MediaType.IMAGE) ? "images" : "audio";
        return folder + "/" + UUID.randomUUID() + ext;
    }

    public void save(String storageKey, byte[] bytes) {
        Path target = baseDir.resolve(storageKey).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storageKey");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("Storage key collision, retry upload", e);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write file", e);
        }
    }

    public Resource loadAsResource(String storageKey) {
        Path file = baseDir.resolve(storageKey).normalize();
        if (!file.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storageKey");
        }
        return new FileSystemResource(file.toFile());
    }

    public void deleteIfExists(String storageKey) {
        Path file = baseDir.resolve(storageKey).normalize();
        if (!file.startsWith(baseDir)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // why: non-critical cleanup; DB is the source of truth for metadata.
        }
    }

    private String chooseExtension(String originalFilename, String contentType) {
        String fromName = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                fromName = originalFilename.substring(dot).toLowerCase();
            }
        }

        if (!fromName.isBlank() && fromName.matches("\\.[a-z0-9]{1,8}")) {
            return fromName;
        }

        if (contentType == null) return "";
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            default -> "";
        };
    }
}
