package com.app.english.controller;

import com.app.english.dto.tasks.CreateTaskRequest;
import com.app.english.dto.tasks.ReplaceTaskVocabularyRequest;
import com.app.english.dto.tasks.TaskResponse;
import com.app.english.dto.tasks.UpdateTaskRequest;
import com.app.english.models.TaskStatus;
import com.app.english.models.TaskType;
import com.app.english.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Creates a reusable task (vocab-based or non-vocab).
    @PostMapping
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request, Authentication auth) {
        return taskService.create(auth.getName(), request);
    }

    // Searches tasks in the user's private library.
    @GetMapping
    public Page<TaskResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable,
            Authentication auth
    ) {
        return taskService.searchMine(auth.getName(), q, type, status, pageable);
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable Long taskId, Authentication auth) {
        return taskService.get(auth.getName(), taskId);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long taskId, @Valid @RequestBody UpdateTaskRequest request, Authentication auth) {
        return taskService.update(auth.getName(), taskId, request);
    }

    // Replaces vocab items for vocab-based tasks. Non-vocab tasks can leave it empty.
    @PutMapping("/{taskId}/vocabulary")
    public TaskResponse replaceVocabulary(
            @PathVariable Long taskId,
            @Valid @RequestBody ReplaceTaskVocabularyRequest request,
            Authentication auth
    ) {
        return taskService.replaceVocabulary(auth.getName(), taskId, request);
    }

    @DeleteMapping("/{taskId}")
    public void delete(@PathVariable Long taskId, Authentication auth) {
        taskService.delete(auth.getName(), taskId);
    }
}
