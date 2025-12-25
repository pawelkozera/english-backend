package com.app.english.models;

import jakarta.persistence.*;

@Entity
@Table(
        name = "lesson_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lesson_position", columnNames = {"lesson_id", "position"})
        },
        indexes = {
                @Index(name = "idx_lesson_items_lesson", columnList = "lesson_id")
        }
)
public class LessonItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private LessonItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(nullable = false)
    private int position;

    protected LessonItem() {}

    public static LessonItem taskItem(Lesson lesson, Task task, int position) {
        LessonItem li = new LessonItem();
        li.lesson = lesson;
        li.itemType = LessonItemType.TASK;
        li.task = task;
        li.position = position;
        return li;
    }

    public Long getId() { return id; }
    public Lesson getLesson() { return lesson; }
    public LessonItemType getItemType() { return itemType; }
    public Task getTask() { return task; }
    public int getPosition() { return position; }
}
