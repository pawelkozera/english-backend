package com.app.english.service;

import com.app.english.dto.tasks.CreateTaskRequest;
import com.app.english.dto.tasks.ReplaceTaskVocabularyRequest;
import com.app.english.dto.tasks.TaskResponse;
import com.app.english.dto.tasks.UpdateTaskRequest;
import com.app.english.exceptions.ForbiddenException;
import com.app.english.exceptions.TaskInUseException;
import com.app.english.exceptions.TaskNotFoundException;
import com.app.english.models.*;
import com.app.english.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskVocabularyRepository taskVocabularyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final LessonItemRepository lessonItemRepository;

    public TaskService(
            TaskRepository taskRepository,
            TaskVocabularyRepository taskVocabularyRepository,
            VocabularyRepository vocabularyRepository,
            UserRepository userRepository,
            LessonItemRepository lessonItemRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskVocabularyRepository = taskVocabularyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
        this.lessonItemRepository = lessonItemRepository;
    }

    @Transactional
    public TaskResponse create(String actorEmail, CreateTaskRequest req) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        TaskStatus status = (req.status() == null) ? TaskStatus.DRAFT : req.status();
        Task task = new Task(req.title().trim(), req.type(), status, req.payload(), actor);
        Task saved = taskRepository.saveAndFlush(task);

        List<Long> vocabIds = normalizeIds(req.vocabularyIds());
        if (!vocabIds.isEmpty()) {
            replaceVocabularyInternal(actor, saved, vocabIds);
        }

        return toResponse(saved, vocabIds.isEmpty() ? List.of() : taskVocabularyRepository.findVocabularyIds(saved.getId()));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(String actorEmail, Long taskId) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        if (!task.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        return toResponse(task, taskVocabularyRepository.findVocabularyIds(taskId));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> searchMine(String actorEmail, String q, TaskType type, TaskStatus status, Pageable pageable) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return taskRepository.searchByOwner(actor.getId(), normalizeNullable(q), type, status, pageable)
                .map(t -> toResponse(t, taskVocabularyRepository.findVocabularyIds(t.getId())));
    }

    @Transactional
    public TaskResponse update(String actorEmail, Long taskId, UpdateTaskRequest req) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        if (!task.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        task.update(req.title().trim(), req.status(), req.payload());
        return toResponse(task, taskVocabularyRepository.findVocabularyIds(taskId));
    }

    @Transactional
    public TaskResponse replaceVocabulary(String actorEmail, Long taskId, ReplaceTaskVocabularyRequest req) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        if (!task.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        List<Long> vocabIds = normalizeIds(req.vocabularyIds());
        replaceVocabularyInternal(actor, task, vocabIds);

        return toResponse(task, taskVocabularyRepository.findVocabularyIds(taskId));
    }

    @Transactional
    public void delete(String actorEmail, Long taskId) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        if (!task.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        boolean usedInLessons = lessonItemRepository.existsByTaskId(taskId);

        if (usedInLessons) {
            throw new TaskInUseException("Cannot delete task: it is used in at least one lesson");
        }

        taskVocabularyRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);
    }

    private void replaceVocabularyInternal(User actor, Task task, List<Long> vocabIds) {
        // Why: we ensure user can only attach vocabulary from their own library.
        if (!vocabIds.isEmpty()) {
            List<Vocabulary> vocab = vocabularyRepository.findAllById(vocabIds);
            if (vocab.size() != vocabIds.size()) {
                throw new IllegalArgumentException("Some vocabularyIds do not exist");
            }
            for (Vocabulary v : vocab) {
                if (!v.getCreatedBy().getId().equals(actor.getId())) {
                    throw new ForbiddenException("Cannot use vocabulary created by another user");
                }
            }
        }

        taskVocabularyRepository.deleteByTaskId(task.getId());

        int pos = 0;
        for (Long vid : vocabIds) {
            Vocabulary v = vocabularyRepository.findById(vid)
                    .orElseThrow(() -> new IllegalArgumentException("Vocabulary not found: " + vid));
            taskVocabularyRepository.save(new TaskVocabulary(task, v, pos++));
        }
    }

    private TaskResponse toResponse(Task t, List<Long> vocabIds) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getType(),
                t.getStatus(),
                t.getPayload(),
                vocabIds,
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private String normalizeNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
