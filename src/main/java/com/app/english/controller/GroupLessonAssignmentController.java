package com.app.english.controller;

import com.app.english.dto.lessons.AssignLessonRequest;
import com.app.english.dto.lessons.LessonAssignmentResponse;
import com.app.english.dto.lessons.ReorderLessonAssignmentsRequest;
import com.app.english.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/lessons")
public class GroupLessonAssignmentController {

    private final LessonService lessonService;

    public GroupLessonAssignmentController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping("/{lessonId}/assign")
    public LessonAssignmentResponse assign(
            @PathVariable Long groupId,
            @PathVariable Long lessonId,
            @Valid @RequestBody AssignLessonRequest req,
            Authentication auth
    ) {
        return lessonService.assignToGroupOrUser(auth.getName(), groupId, lessonId, req);
    }

    @GetMapping("/assignments")
    public Page<LessonAssignmentResponse> listAssignments(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long userId,
            Pageable pageable,
            Authentication auth
    ) {
        return lessonService.pageTeacherAssignments(auth.getName(), groupId, userId, pageable);
    }

    // Drag & drop reorder (bucket-specific):
    // - userId=null => group-wide assignments
    // - userId=123  => per-user assignments for that user
    @PutMapping("/assignments/order")
    public void reorder(
            @PathVariable Long groupId,
            @Valid @RequestBody ReorderLessonAssignmentsRequest req,
            Authentication auth
    ) {
        lessonService.reorderAssignmentsForGroup(auth.getName(), groupId, req);
    }
}

