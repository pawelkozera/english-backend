package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "lesson_task_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ltp_progress_task", columnNames = {"progress_id", "task_id"})
        },
        indexes = {
                @Index(name = "idx_ltp_progress", columnList = "progress_id"),
                @Index(name = "idx_ltp_task", columnList = "task_id")
        }
)
public class LessonTaskProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // parent progress
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "progress_id", nullable = false)
    private LessonProgress progress;

    // task being tracked
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected LessonTaskProgress() {}

    public LessonTaskProgress(LessonProgress progress, Task task) {
        this.progress = progress;
        this.task = task;
        this.completed = false;
    }

    public Long getId() { return id; }
    public LessonProgress getProgress() { return progress; }
    public Task getTask() { return task; }
    public boolean isCompleted() { return completed; }
    public Instant getCompletedAt() { return completedAt; }

    public void markCompleted() {
        if (!completed) {
            completed = true;
            completedAt = Instant.now();
        }
    }
}
