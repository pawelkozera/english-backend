package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "lessons",
        indexes = {
                @Index(name = "idx_lessons_owner_updated", columnList = "created_by_id, updated_at"),
                @Index(name = "idx_lessons_owner_status", columnList = "created_by_id, status")
        }
)
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LessonStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {}

    public Lesson(String title, String description, LessonStatus status, User createdBy) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = LessonStatus.DRAFT;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LessonStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, String description, LessonStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public void archive() {
        this.status = LessonStatus.ARCHIVED;
    }

    public boolean isArchived() {
        return this.status == LessonStatus.ARCHIVED;
    }
}
