package com.app.english.models;

import jakarta.persistence.*;

@Entity
@Table(
        name = "task_vocabulary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_task_vocab", columnNames = {"task_id", "vocabulary_id"})
        },
        indexes = {
                @Index(name = "idx_task_vocab_task", columnList = "task_id")
        }
)
public class TaskVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(nullable = false)
    private int position;

    protected TaskVocabulary() {}

    public TaskVocabulary(Task task, Vocabulary vocabulary, int position) {
        this.task = task;
        this.vocabulary = vocabulary;
        this.position = position;
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public Vocabulary getVocabulary() { return vocabulary; }
    public int getPosition() { return position; }
}
