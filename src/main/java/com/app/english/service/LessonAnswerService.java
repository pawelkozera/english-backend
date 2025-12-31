package com.app.english.service;

import com.app.english.dto.lessons.LessonTaskAnswerRequest;
import com.app.english.dto.lessons.LessonTaskAnswerResponse;
import com.app.english.exceptions.ForbiddenException;
import com.app.english.exceptions.GroupNotFoundException;
import com.app.english.models.*;
import com.app.english.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LessonAnswerService {

    private final LessonAssignmentRepository lessonAssignmentRepository;
    private final LessonItemRepository lessonItemRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonTaskAnswerRepository lessonTaskAnswerRepository;
    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public LessonAnswerService(
            LessonAssignmentRepository lessonAssignmentRepository,
            LessonItemRepository lessonItemRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonTaskAnswerRepository lessonTaskAnswerRepository,
            MembershipRepository membershipRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            ObjectMapper objectMapper
    ) {
        this.lessonAssignmentRepository = lessonAssignmentRepository;
        this.lessonItemRepository = lessonItemRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonTaskAnswerRepository = lessonTaskAnswerRepository;
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    private JsonNode toJsonNode(Object value) {
        return (value == null) ? null : objectMapper.valueToTree(value);
    }

    // -------------------------
    // Student: my answer
    // -------------------------

    @Transactional(readOnly = true)
    public LessonTaskAnswerResponse getMyAnswer(String actorEmail, Long assignmentId, Long taskId) {
        User actor = requireActor(actorEmail);

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        enforceStudentCanAccessAssignment(actor, a);
        enforceTaskBelongsToLesson(a.getLesson().getId(), taskId);

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(actor.getId(), assignmentId)
                .orElse(null);

        if (p == null) {
            return new LessonTaskAnswerResponse(assignmentId, taskId, LessonAnswerStatus.DRAFT, null, null, null);
        }

        LessonTaskAnswer ans = lessonTaskAnswerRepository.findByProgressIdAndTaskId(p.getId(), taskId)
                .orElse(null);

        if (ans == null) {
            return new LessonTaskAnswerResponse(assignmentId, taskId, LessonAnswerStatus.DRAFT, null, null, null);
        }

        return toResponse(assignmentId, taskId, ans);
    }

    @Transactional
    public LessonTaskAnswerResponse saveDraft(String actorEmail, Long assignmentId, Long taskId, LessonTaskAnswerRequest req) {
        User actor = requireActor(actorEmail);

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        enforceStudentCanAccessAssignment(actor, a);
        enforceTaskBelongsToLesson(a.getLesson().getId(), taskId);

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(actor.getId(), assignmentId)
                .orElseGet(() -> lessonProgressRepository.saveAndFlush(new LessonProgress(actor, a)));

        p.markInProgress();
        lessonProgressRepository.save(p);

        JsonNode payload = toJsonNode(req.answer());

        LessonTaskAnswer ans = lessonTaskAnswerRepository.findByProgressIdAndTaskId(p.getId(), taskId)
                .orElseGet(() -> {
                    Task task = taskRepository.findById(taskId)
                            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                    return new LessonTaskAnswer(p, task, payload);
                });

        ans.saveDraft(payload);
        LessonTaskAnswer saved = lessonTaskAnswerRepository.saveAndFlush(ans);

        return toResponse(assignmentId, taskId, saved);
    }

    @Transactional
    public LessonTaskAnswerResponse submit(String actorEmail, Long assignmentId, Long taskId, LessonTaskAnswerRequest req) {
        User actor = requireActor(actorEmail);

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        enforceStudentCanAccessAssignment(actor, a);
        enforceTaskBelongsToLesson(a.getLesson().getId(), taskId);

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(actor.getId(), assignmentId)
                .orElseGet(() -> lessonProgressRepository.saveAndFlush(new LessonProgress(actor, a)));

        p.markInProgress();
        lessonProgressRepository.save(p);

        JsonNode payload = toJsonNode(req.answer());

        LessonTaskAnswer ans = lessonTaskAnswerRepository.findByProgressIdAndTaskId(p.getId(), taskId)
                .orElseGet(() -> {
                    Task task = taskRepository.findById(taskId)
                            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                    return new LessonTaskAnswer(p, task, payload);
                });

        ans.submit(payload); // resubmit allowed
        LessonTaskAnswer saved = lessonTaskAnswerRepository.saveAndFlush(ans);

        return toResponse(assignmentId, taskId, saved);
    }

    // -------------------------
    // Teacher: view student's answer (read-only)
    // -------------------------

    @Transactional(readOnly = true)
    public LessonTaskAnswerResponse getStudentAnswer(
            String teacherEmail,
            Long groupId,
            Long assignmentId,
            Long taskId,
            Long studentUserId
    ) {
        Membership teacherMembership = membershipRepository.findByUserEmailAndGroupId(teacherEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (teacherMembership.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (!a.getGroup().getId().equals(groupId)) {
            throw new ForbiddenException("Assignment does not belong to this group");
        }

        if (!membershipRepository.existsByUserIdAndGroupId(studentUserId, groupId)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }

        if (a.getAssignedToUser() != null && !a.getAssignedToUser().getId().equals(studentUserId)) {
            throw new ForbiddenException("This assignment is for another user");
        }

        enforceTaskBelongsToLesson(a.getLesson().getId(), taskId);

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(studentUserId, assignmentId)
                .orElse(null);

        if (p == null) {
            return new LessonTaskAnswerResponse(assignmentId, taskId, LessonAnswerStatus.DRAFT, null, null, null);
        }

        LessonTaskAnswer ans = lessonTaskAnswerRepository.findByProgressIdAndTaskId(p.getId(), taskId)
                .orElse(null);

        if (ans == null) {
            return new LessonTaskAnswerResponse(assignmentId, taskId, LessonAnswerStatus.DRAFT, null, null, null);
        }

        return toResponse(assignmentId, taskId, ans);
    }

    // -------------------------
    // helpers
    // -------------------------

    private LessonTaskAnswerResponse toResponse(Long assignmentId, Long taskId, LessonTaskAnswer ans) {
        Object plain = (ans.getAnswerJson() == null)
                ? null
                : objectMapper.convertValue(ans.getAnswerJson(), Object.class);

        return new LessonTaskAnswerResponse(
                assignmentId,
                taskId,
                ans.getStatus(),
                plain,
                ans.getUpdatedAt(),
                ans.getSubmittedAt()
        );
    }

    private User requireActor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private void enforceTaskBelongsToLesson(Long lessonId, Long taskId) {
        List<Long> ids = lessonItemRepository.findTaskIdsForLesson(lessonId);
        if (!ids.contains(taskId)) {
            throw new IllegalArgumentException("Task does not belong to this lesson");
        }
    }

    private void enforceStudentCanAccessAssignment(User actor, LessonAssignment a) {
        boolean isMember = membershipRepository.existsByUserIdAndGroupId(actor.getId(), a.getGroup().getId());
        if (!isMember) {
            if (!groupRepository.existsById(a.getGroup().getId())) throw new GroupNotFoundException("Group not found");
            throw new ForbiddenException("Not a member of this group");
        }

        if (a.getAssignedToUser() != null && !a.getAssignedToUser().getId().equals(actor.getId())) {
            throw new ForbiddenException("This lesson is assigned to another user");
        }

        Instant now = Instant.now();
        if (a.getVisibleFrom() != null && a.getVisibleFrom().isAfter(now)) {
            throw new ForbiddenException("Lesson not visible yet");
        }
        if (a.getVisibleTo() != null && a.getVisibleTo().isBefore(now)) {
            throw new ForbiddenException("Lesson is no longer visible");
        }

        if (a.getLesson().getStatus() == LessonStatus.ARCHIVED) {
            throw new ForbiddenException("Lesson is archived");
        }
    }
}