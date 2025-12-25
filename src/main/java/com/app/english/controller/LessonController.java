package com.app.english.controller;

import com.app.english.dto.lessons.*;
import com.app.english.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping
    public LessonResponse create(@Valid @RequestBody CreateLessonRequest req, Authentication auth) {
        return lessonService.create(auth.getName(), req);
    }

    @GetMapping
    public Page<LessonResponse> listMine(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) String q,
            Pageable pageable,
            Authentication auth
    ) {
        return lessonService.listMine(auth.getName(), pageable, includeArchived, q);
    }

    @GetMapping("/{lessonId}")
    public LessonResponse get(@PathVariable Long lessonId, Authentication auth) {
        return lessonService.get(auth.getName(), lessonId);
    }

    @PutMapping("/{lessonId}")
    public LessonResponse update(@PathVariable Long lessonId, @Valid @RequestBody UpdateLessonRequest req, Authentication auth) {
        return lessonService.update(auth.getName(), lessonId, req);
    }

    // Replaces the entire lesson content with a new ordered list of task ids.
    @PutMapping("/{lessonId}/items")
    public LessonResponse replaceItems(@PathVariable Long lessonId, @Valid @RequestBody ReplaceLessonItemsRequest req, Authentication auth) {
        return lessonService.replaceItems(auth.getName(), lessonId, req);
    }

    // Student tab: group-wide (across my groups)
    @GetMapping("/assignments/me/group")
    public Page<LessonAssignmentResponse> myGroupAssignments(Authentication auth, Pageable pageable) {
        return lessonService.pageMyGroupWideAssignments(auth.getName(), pageable);
    }

    // Student tab: personal (assigned specifically to me)
    @GetMapping("/assignments/me/personal")
    public Page<LessonAssignmentResponse> myPersonalAssignments(Authentication auth, Pageable pageable) {
        return lessonService.pageMyPersonalAssignments(auth.getName(), pageable);
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> archive(@PathVariable Long lessonId, Authentication auth) {
        lessonService.archive(auth.getName(), lessonId);
        return ResponseEntity.noContent().build();
    }
}
