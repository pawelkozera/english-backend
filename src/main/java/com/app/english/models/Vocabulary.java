package com.app.english.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "vocabulary",
        indexes = {
                @Index(name = "idx_vocab_owner_created_at", columnList = "created_by_id, created_at"),
                @Index(name = "idx_vocab_owner_term_en", columnList = "created_by_id, term_en"),
                @Index(name = "idx_vocab_owner_term_pl", columnList = "created_by_id, term_pl")
        }
)
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_en", nullable = false, length = 200)
    private String termEn;

    @Column(name = "term_pl", nullable = false, length = 200)
    private String termPl;

    @Column(name = "example_en", length = 1000)
    private String exampleEn;

    @Column(name = "example_pl", length = 1000)
    private String examplePl;

    @Column(name = "image_media_id")
    private Long imageMediaId;

    @Column(name = "audio_media_id")
    private Long audioMediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vocabulary() {}

    public Vocabulary(
            String termEn,
            String termPl,
            String exampleEn,
            String examplePl,
            Long imageMediaId,
            Long audioMediaId,
            User createdBy
    ) {
        this.termEn = termEn;
        this.termPl = termPl;
        this.exampleEn = exampleEn;
        this.examplePl = examplePl;
        this.imageMediaId = imageMediaId;
        this.audioMediaId = audioMediaId;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTermEn() { return termEn; }
    public String getTermPl() { return termPl; }
    public String getExampleEn() { return exampleEn; }
    public String getExamplePl() { return examplePl; }
    public Long getImageMediaId() { return imageMediaId; }
    public Long getAudioMediaId() { return audioMediaId; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            String termEn,
            String termPl,
            String exampleEn,
            String examplePl,
            Long imageMediaId,
            Long audioMediaId
    ) {
        this.termEn = termEn;
        this.termPl = termPl;
        this.exampleEn = exampleEn;
        this.examplePl = examplePl;
        this.imageMediaId = imageMediaId;
        this.audioMediaId = audioMediaId;
    }
}
