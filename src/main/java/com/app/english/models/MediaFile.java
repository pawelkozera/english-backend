package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "media_files",
        indexes = {
                @Index(name = "idx_media_owner_created_at", columnList = "created_by_id, created_at"),
                @Index(name = "idx_media_storage_key", columnList = "storage_key")
        }
)
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(name = "storage_key", nullable = false, unique = true, length = 400)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaFile() {}

    public MediaFile(
            MediaType type,
            String storageKey,
            String originalName,
            String contentType,
            long size,
            User createdBy
    ) {
        this.type = type;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.size = size;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public MediaType getType() { return type; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
