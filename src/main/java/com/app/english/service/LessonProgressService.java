package com.app.english.service;

import com.app.english.dto.lessons.LessonProgressResponse;
import com.app.english.exceptions.ForbiddenException;
import com.app.english.exceptions.GroupNotFoundException;
import com.app.english.models.*;
import com.app.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LessonProgressService {

    private final LessonAssignmentRepository lessonAssignmentRepository;
    private final LessonItemRepository lessonItemRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonTaskProgressRepository lessonTaskProgressRepository;
    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public LessonProgressService(
            LessonAssignmentRepository lessonAssignmentRepository,
            LessonItemRepository lessonItemRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonTaskProgressRepository lessonTaskProgressRepository,
            MembershipRepository membershipRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            TaskRepository taskRepository
    ) {
        this.lessonAssignmentRepository = lessonAssignmentRepository;
        this.lessonItemRepository = lessonItemRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonTaskProgressRepository = lessonTaskProgressRepository;
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    // -------------------------
    // Student: my progress
    // -------------------------

    @Transactional(readOnly = true)
    public LessonProgressResponse getProgress(String actorEmail, Long assignmentId) {
        User actor = requireActor(actorEmail);

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        enforceStudentCanAccessAssignment(actor, a);

        List<Long> lessonTaskIds = lessonItemRepository.findTaskIdsForLesson(a.getLesson().getId());
        int total = new HashSet<>(lessonTaskIds).size();

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(actor.getId(), assignmentId)
                .orElse(null);

        if (p == null) {
            return new LessonProgressResponse(
                    assignmentId,
                    a.getLesson().getId(),
                    LessonProgressStatus.NOT_STARTED,
                    null,
                    null,
                    Set.of(),
                    0,
                    total
            );
        }

        Set<Long> completed = lessonTaskProgressRepository.findCompletedTaskIds(p.getId());
        int done = countDone(lessonTaskIds, completed);

        return new LessonProgressResponse(
                assignmentId,
                a.getLesson().getId(),
                p.getStatus(),
                p.getStartedAt(),
                p.getCompletedAt(),
                completed,
                done,
                total
        );
    }

    @Transactional
    public LessonProgressResponse markTaskCompleted(String actorEmail, Long assignmentId, Long taskId) {
        User actor = requireActor(actorEmail);

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        enforceStudentCanAccessAssignment(actor, a);

        List<Long> lessonTaskIds = lessonItemRepository.findTaskIdsForLesson(a.getLesson().getId());
        if (!lessonTaskIds.contains(taskId)) {
            throw new IllegalArgumentException("Task does not belong to this lesson");
        }

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(actor.getId(), assignmentId)
                .orElseGet(() -> lessonProgressRepository.save(new LessonProgress(actor, a)));

        p.markInProgress();

        LessonTaskProgress tp = lessonTaskProgressRepository.findByProgressIdAndTaskId(p.getId(), taskId)
                .orElseGet(() -> {
                    Task task = taskRepository.findById(taskId)
                            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                    return lessonTaskProgressRepository.save(new LessonTaskProgress(p, task));
                });

        tp.markCompleted();
        lessonTaskProgressRepository.save(tp);

        Set<Long> completed = new HashSet<>(lessonTaskProgressRepository.findCompletedTaskIds(p.getId()));
        int total = new HashSet<>(lessonTaskIds).size();
        int done = countDone(lessonTaskIds, completed);

        if (total > 0 && done == total) {
            p.markCompleted();
        }
        lessonProgressRepository.save(p);

        return new LessonProgressResponse(
                assignmentId,
                a.getLesson().getId(),
                p.getStatus(),
                p.getStartedAt(),
                p.getCompletedAt(),
                completed,
                done,
                total
        );
    }

    @Transactional
    public LessonProgressResponse markLessonCompleted(String actorEmail, Long assignmentId) {
        User actor = requireActor(actorEmail);

        LessonAssignment a = lessonAssignmentRepository.findByIdWithLessonAndGroup(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        enforceStudentCanAccessAssignment(actor, a);

        List<Long> lessonTaskIds = lessonItemRepository.findTaskIdsForLesson(a.getLesson().getId());
        int total = new HashSet<>(lessonTaskIds).size();

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(actor.getId(), assignmentId)
                .orElseGet(() -> lessonProgressRepository.save(new LessonProgress(actor, a)));

        p.markCompleted();
        lessonProgressRepository.save(p);

        Set<Long> completed = new HashSet<>(lessonTaskProgressRepository.findCompletedTaskIds(p.getId()));
        int done = countDone(lessonTaskIds, completed);

        return new LessonProgressResponse(
                assignmentId,
                a.getLesson().getId(),
                p.getStatus(),
                p.getStartedAt(),
                p.getCompletedAt(),
                completed,
                done,
                total
        );
    }

    // -------------------------
    // Teacher: view student's progress (read-only)
    // -------------------------

    @Transactional(readOnly = true)
    public LessonProgressResponse getProgressForStudent(
            String teacherEmail,
            Long groupId,
            Long assignmentId,
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

        // If assignment is per-user, teacher can only view for that same user.
        if (a.getAssignedToUser() != null && !a.getAssignedToUser().getId().equals(studentUserId)) {
            throw new ForbiddenException("This assignment is for another user");
        }

        List<Long> lessonTaskIds = lessonItemRepository.findTaskIdsForLesson(a.getLesson().getId());
        int total = new HashSet<>(lessonTaskIds).size();

        LessonProgress p = lessonProgressRepository.findByUserIdAndAssignmentId(studentUserId, assignmentId)
                .orElse(null);

        if (p == null) {
            return new LessonProgressResponse(
                    assignmentId,
                    a.getLesson().getId(),
                    LessonProgressStatus.NOT_STARTED,
                    null,
                    null,
                    Set.of(),
                    0,
                    total
            );
        }

        Set<Long> completed = lessonTaskProgressRepository.findCompletedTaskIds(p.getId());
        int done = countDone(lessonTaskIds, completed);

        return new LessonProgressResponse(
                assignmentId,
                a.getLesson().getId(),
                p.getStatus(),
                p.getStartedAt(),
                p.getCompletedAt(),
                completed,
                done,
                total
        );
    }

    // -------------------------
    // helpers
    // -------------------------

    private int countDone(List<Long> lessonTaskIds, Set<Long> completedTaskIds) {
        // count unique tasks
        Set<Long> unique = new HashSet<>(lessonTaskIds);
        int done = 0;
        for (Long id : unique) {
            if (completedTaskIds.contains(id)) done++;
        }
        return done;
    }

    private User requireActor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
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