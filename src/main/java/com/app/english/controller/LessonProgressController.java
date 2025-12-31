package com.app.english.controller;

import com.app.english.dto.lessons.LessonProgressResponse;
import com.app.english.service.LessonProgressService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class LessonProgressController {

    private final LessonProgressService lessonProgressService;

    public LessonProgressController(LessonProgressService lessonProgressService) {
        this.lessonProgressService = lessonProgressService;
    }

    // Student: my progress
    @GetMapping("/api/lesson-assignments/{assignmentId}/progress")
    public LessonProgressResponse getProgress(@PathVariable Long assignmentId, Authentication auth) {
        return lessonProgressService.getProgress(auth.getName(), assignmentId);
    }

    @PostMapping("/api/lesson-assignments/{assignmentId}/tasks/{taskId}/complete")
    public LessonProgressResponse completeTask(@PathVariable Long assignmentId, @PathVariable Long taskId, Authentication auth) {
        return lessonProgressService.markTaskCompleted(auth.getName(), assignmentId, taskId);
    }

    @PostMapping("/api/lesson-assignments/{assignmentId}/complete")
    public LessonProgressResponse completeLesson(@PathVariable Long assignmentId, Authentication auth) {
        return lessonProgressService.markLessonCompleted(auth.getName(), assignmentId);
    }

    // Teacher: view student's progress (read-only)
    // GET /api/groups/{groupId}/lesson-assignments/{assignmentId}/progress?userId=123
    @GetMapping("/api/groups/{groupId}/lesson-assignments/{assignmentId}/progress")
    public LessonProgressResponse getStudentProgress(
            @PathVariable Long groupId,
            @PathVariable Long assignmentId,
            @RequestParam Long userId,
            Authentication auth
    ) {
        return lessonProgressService.getProgressForStudent(auth.getName(), groupId, assignmentId, userId);
    }
}