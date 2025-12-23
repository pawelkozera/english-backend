package com.app.english.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_owner_updated", columnList = "created_by_id, updated_at"),
                @Index(name = "idx_tasks_owner_type", columnList = "created_by_id, type"),
                @Index(name = "idx_tasks_owner_status", columnList = "created_by_id, status")
        }
)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;

    // Type-specific configuration (e.g. prompt, minWords, direction, shuffle).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {}

    public Task(String title, TaskType type, TaskStatus status, Map<String, Object> payload, User createdBy) {
        this.title = title;
        this.type = type;
        this.status = status;
        this.payload = (payload == null) ? new HashMap<>() : new HashMap<>(payload);
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = TaskStatus.DRAFT;
        if (this.payload == null) this.payload = new HashMap<>();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.payload == null) this.payload = new HashMap<>();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public TaskType getType() { return type; }
    public TaskStatus getStatus() { return status; }
    public Map<String, Object> getPayload() { return payload; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, TaskStatus status, Map<String, Object> payload) {
        this.title = title;
        this.status = status;
        this.payload = (payload == null) ? new HashMap<>() : new HashMap<>(payload);
    }
}
