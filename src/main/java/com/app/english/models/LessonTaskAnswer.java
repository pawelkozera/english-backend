package com.app.english.models;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "lesson_task_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lesson_answer_progress_task", columnNames = {"progress_id", "task_id"})
        },
        indexes = {
                @Index(name = "idx_lesson_answer_progress", columnList = "progress_id, updated_at"),
                @Index(name = "idx_lesson_answer_task", columnList = "task_id")
        }
)
public class LessonTaskAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Context: user + assignment (via LessonProgress)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "progress_id", nullable = false)
    private LessonProgress progress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonAnswerStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_json", columnDefinition = "jsonb")
    private JsonNode answerJson;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonTaskAnswer() {}

    public LessonTaskAnswer(LessonProgress progress, Task task, JsonNode answerJson) {
        this.progress = progress;
        this.task = task;
        this.answerJson = answerJson;
        this.status = LessonAnswerStatus.DRAFT;
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
    public LessonProgress getProgress() { return progress; }
    public Task getTask() { return task; }
    public LessonAnswerStatus getStatus() { return status; }
    public JsonNode getAnswerJson() { return answerJson; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void saveDraft(JsonNode answerJson) {
        this.answerJson = answerJson;
        this.status = LessonAnswerStatus.DRAFT;
        // submittedAt stays as-is; training resubmits are handled via submit()
    }

    public void submit(JsonNode answerJson) {
        this.answerJson = answerJson;
        this.status = LessonAnswerStatus.SUBMITTED;
        this.submittedAt = Instant.now(); // resubmit allowed => overwrite timestamp
    }
}
