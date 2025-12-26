package com.app.english.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "lesson_assignments",
        indexes = {
                @Index(name = "idx_lesson_assign_group", columnList = "group_id, display_order, created_at"),
                @Index(name = "idx_lesson_assign_user", columnList = "assigned_to_user_id, display_order, created_at"),
                @Index(name = "idx_lesson_assign_lesson", columnList = "lesson_id, created_at")
        }
)
public class LessonAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Assignment always belongs to a group. Per-user assignment is within this group.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    // Null => visible for the whole group. Non-null => only for that user (within the group).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedBy;

    @Column(name = "visible_from")
    private Instant visibleFrom;

    @Column(name = "visible_to")
    private Instant visibleTo;

    // Lower = higher in the grid.
    @Column(name = "display_order", nullable = false)
    private long displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LessonAssignment() {}

    public LessonAssignment(
            Group group,
            Lesson lesson,
            User assignedToUser,
            User assignedBy,
            Instant visibleFrom,
            Instant visibleTo,
            long displayOrder
    ) {
        this.group = group;
        this.lesson = lesson;
        this.assignedToUser = assignedToUser;
        this.assignedBy = assignedBy;
        this.visibleFrom = visibleFrom;
        this.visibleTo = visibleTo;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public Lesson getLesson() { return lesson; }
    public User getAssignedToUser() { return assignedToUser; }
    public User getAssignedBy() { return assignedBy; }
    public Instant getVisibleFrom() { return visibleFrom; }
    public Instant getVisibleTo() { return visibleTo; }
    public long getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDisplayOrder(long displayOrder) { this.displayOrder = displayOrder; }
    public void setVisibleFrom(Instant visibleFrom) {
        this.visibleFrom = visibleFrom;
    }
    public void setVisibleTo(Instant visibleTo) {
        this.visibleTo = visibleTo;
    }
}