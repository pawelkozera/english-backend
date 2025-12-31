package com.app.english.controller;

import com.app.english.dto.lessons.LessonTaskAnswerRequest;
import com.app.english.dto.lessons.LessonTaskAnswerResponse;
import com.app.english.service.LessonAnswerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class LessonAnswerController {

    private final LessonAnswerService lessonAnswerService;

    public LessonAnswerController(LessonAnswerService lessonAnswerService) {
        this.lessonAnswerService = lessonAnswerService;
    }

    // Student: my answer
    @GetMapping("/api/lesson-assignments/{assignmentId}/tasks/{taskId}/answer")
    public LessonTaskAnswerResponse getMy(
            @PathVariable Long assignmentId,
            @PathVariable Long taskId,
            Authentication auth
    ) {
        return lessonAnswerService.getMyAnswer(auth.getName(), assignmentId, taskId);
    }

    @PutMapping("/api/lesson-assignments/{assignmentId}/tasks/{taskId}/answer")
    public LessonTaskAnswerResponse saveDraft(
            @PathVariable Long assignmentId,
            @PathVariable Long taskId,
            @RequestBody LessonTaskAnswerRequest req,
            Authentication auth
    ) {
        return lessonAnswerService.saveDraft(auth.getName(), assignmentId, taskId, req);
    }

    @PostMapping("/api/lesson-assignments/{assignmentId}/tasks/{taskId}/submit")
    public LessonTaskAnswerResponse submit(
            @PathVariable Long assignmentId,
            @PathVariable Long taskId,
            @RequestBody LessonTaskAnswerRequest req,
            Authentication auth
    ) {
        return lessonAnswerService.submit(auth.getName(), assignmentId, taskId, req);
    }

    // Teacher: view student's answer (read-only)
    // GET /api/groups/{groupId}/lesson-assignments/{assignmentId}/tasks/{taskId}/answer?userId=123
    @GetMapping("/api/groups/{groupId}/lesson-assignments/{assignmentId}/tasks/{taskId}/answer")
    public LessonTaskAnswerResponse teacherView(
            @PathVariable Long groupId,
            @PathVariable Long assignmentId,
            @PathVariable Long taskId,
            @RequestParam Long userId,
            Authentication auth
    ) {
        return lessonAnswerService.getStudentAnswer(auth.getName(), groupId, assignmentId, taskId, userId);
    }
}
