package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lesson_progress_user_assignment", columnNames = {"user_id", "assignment_id"})
        },
        indexes = {
                @Index(name = "idx_lesson_progress_user", columnList = "user_id, updated_at"),
                @Index(name = "idx_lesson_progress_assignment", columnList = "assignment_id, updated_at")
        }
)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Student who is solving
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Which assignment instance (group-wide or per-user)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private LessonAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonProgressStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonProgress() {}

    public LessonProgress(User user, LessonAssignment assignment) {
        this.user = user;
        this.assignment = assignment;
        this.status = LessonProgressStatus.NOT_STARTED;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LessonAssignment getAssignment() { return assignment; }
    public LessonProgressStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markInProgress() {
        if (status == LessonProgressStatus.NOT_STARTED) {
            status = LessonProgressStatus.IN_PROGRESS;
            startedAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    public void markCompleted() {
        if (status != LessonProgressStatus.COMPLETED) {
            status = LessonProgressStatus.COMPLETED;
            if (startedAt == null) startedAt = Instant.now();
            completedAt = Instant.now();
        }
        updatedAt = Instant.now();
    }
}
