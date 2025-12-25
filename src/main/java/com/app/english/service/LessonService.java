package com.app.english.service;

import com.app.english.dto.lessons.*;
import com.app.english.exceptions.ForbiddenException;
import com.app.english.exceptions.GroupNotFoundException;
import com.app.english.exceptions.LessonNotFoundException;
import com.app.english.models.*;
import com.app.english.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonItemRepository lessonItemRepository;
    private final LessonAssignmentRepository lessonAssignmentRepository;

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;

    public LessonService(
            LessonRepository lessonRepository,
            LessonItemRepository lessonItemRepository,
            LessonAssignmentRepository lessonAssignmentRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            GroupRepository groupRepository,
            MembershipRepository membershipRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.lessonItemRepository = lessonItemRepository;
        this.lessonAssignmentRepository = lessonAssignmentRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public LessonResponse create(String actorEmail, CreateLessonRequest req) {
        User actor = requireActor(actorEmail);

        LessonStatus status = (req.status() == null) ? LessonStatus.DRAFT : req.status();
        Lesson saved = lessonRepository.save(new Lesson(
                req.title().trim(),
                normalizeNullable(req.description()),
                status,
                actor
        ));

        return toLessonResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public Page<LessonResponse> listMine(String actorEmail, Pageable pageable, boolean includeArchived, String q) {
        User actor = requireActor(actorEmail);

        List<LessonStatus> statuses = includeArchived
                ? List.of(LessonStatus.DRAFT, LessonStatus.PUBLISHED, LessonStatus.ARCHIVED)
                : List.of(LessonStatus.DRAFT, LessonStatus.PUBLISHED);

        String query = (q == null || q.trim().isEmpty()) ? null : q.trim();

        var page = (query == null)
                ? lessonRepository.findByCreatedByIdAndStatusInOrderByUpdatedAtDesc(actor.getId(), statuses, pageable)
                : lessonRepository.findByCreatedByIdAndStatusInAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(actor.getId(), statuses, query, pageable);

        return page.map(l -> toLessonResponse(l, loadItems(l.getId())));
    }

    @Transactional(readOnly = true)
    public LessonResponse get(String actorEmail, Long lessonId) {
        User actor = requireActor(actorEmail);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));

        if (!lesson.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        return toLessonResponse(lesson, loadItems(lessonId));
    }

    @Transactional
    public LessonResponse update(String actorEmail, Long lessonId, UpdateLessonRequest req) {
        User actor = requireActor(actorEmail);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));

        if (!lesson.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        lesson.update(req.title().trim(), normalizeNullable(req.description()), req.status());
        return toLessonResponse(lesson, loadItems(lessonId));
    }

    @Transactional
    public LessonResponse replaceItems(String actorEmail, Long lessonId, ReplaceLessonItemsRequest req) {
        User actor = requireActor(actorEmail);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));

        if (!lesson.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        List<Long> taskIds = normalizeIds(req.taskIds());
        if (taskIds.isEmpty()) {
            lessonItemRepository.deleteByLessonId(lessonId);
            return toLessonResponse(lesson, List.of());
        }

        List<Task> tasks = taskRepository.findAllById(taskIds);
        if (tasks.size() != taskIds.size()) {
            throw new IllegalArgumentException("Some taskIds do not exist");
        }

        for (Task t : tasks) {
            if (!t.getCreatedBy().getId().equals(actor.getId())) {
                throw new ForbiddenException("Cannot use tasks created by another user");
            }
        }

        Map<Long, Task> byId = new HashMap<>();
        for (Task t : tasks) byId.put(t.getId(), t);

        lessonItemRepository.deleteByLessonId(lessonId);

        int pos = 0;
        for (Long tid : taskIds) {
            lessonItemRepository.save(LessonItem.taskItem(lesson, byId.get(tid), pos++));
        }

        return toLessonResponse(lesson, loadItems(lessonId));
    }

    @Transactional
    public LessonAssignmentResponse assignToGroupOrUser(String actorEmail, Long groupId, Long lessonId, AssignLessonRequest req) {
        User actor = requireActor(actorEmail);

        Membership teacherMembership = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (teacherMembership.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));

        if (lesson.isArchived()) {
            throw new ForbiddenException("Cannot assign archived lesson");
        }

        if (!lesson.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Cannot assign lesson created by another user");
        }

        Group group = teacherMembership.getGroup();

        User assignedTo = null;
        Long assignedToUserId = req.assignedToUserId();
        if (assignedToUserId != null) {
            boolean isMember = membershipRepository.existsByUserIdAndGroupId(assignedToUserId, groupId);
            if (!isMember) throw new IllegalArgumentException("Assigned user is not a member of this group");
            assignedTo = userRepository.findById(assignedToUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Assigned user not found"));
        }

        // New on top: displayOrder = (min - 1) in the bucket (groupId + assignedToUserId/null)
        Long min = lessonAssignmentRepository.findMinDisplayOrder(groupId, assignedToUserId);
        long displayOrder = (min == null) ? 0L : (min - 1L);

        LessonAssignment saved = lessonAssignmentRepository.save(new LessonAssignment(
                group,
                lesson,
                assignedTo,
                actor,
                req.visibleFrom(),
                req.visibleTo(),
                displayOrder
        ));

        return toAssignmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<LessonAssignmentResponse> pageMyGroupWideAssignments(String actorEmail, Pageable pageable) {
        User actor = requireActor(actorEmail);
        Instant now = Instant.now();

        List<Long> groupIds = membershipRepository.findGroupIdsByUserId(actor.getId());
        if (groupIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return lessonAssignmentRepository.pageStudentGroupWide(groupIds, now, LessonStatus.ARCHIVED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LessonAssignmentResponse> pageMyPersonalAssignments(String actorEmail, Pageable pageable) {
        User actor = requireActor(actorEmail);
        Instant now = Instant.now();
        return lessonAssignmentRepository.pageStudentPersonal(actor.getId(), now, LessonStatus.ARCHIVED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LessonAssignmentResponse> pageTeacherAssignments(String actorEmail, Long groupId, Long userIdOrNull, Pageable pageable) {
        Membership m = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (m.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        if (userIdOrNull == null) {
            return lessonAssignmentRepository.pageTeacherGroupWide(groupId, pageable);
        }

        // ensure target is a member of the group
        if (!membershipRepository.existsByUserIdAndGroupId(userIdOrNull, groupId)) {
            throw new IllegalArgumentException("Assigned user is not a member of this group");
        }

        return lessonAssignmentRepository.pageTeacherPerUser(groupId, userIdOrNull, pageable);
    }

    @Transactional(readOnly = true)
    public List<LessonAssignmentResponse> listAssignmentsForGroup(String actorEmail, Long groupId, Long userIdOrNull) {
        Membership m = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (m.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        return lessonAssignmentRepository.findForGroup(groupId, userIdOrNull)
                .stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Transactional
    public void reorderAssignmentsForGroup(String actorEmail, Long groupId, ReorderLessonAssignmentsRequest req) {
        Membership m = membershipRepository.findByUserEmailAndGroupId(actorEmail, groupId)
                .orElseThrow(() -> {
                    if (!groupRepository.existsById(groupId)) return new GroupNotFoundException("Group not found");
                    return new ForbiddenException("Not a member of this group");
                });

        if (m.getRole() != GroupRole.TEACHER) {
            throw new ForbiddenException("Teacher role required");
        }

        List<Long> ids = normalizeIds(req.assignmentIds());
        if (ids.isEmpty()) return;

        List<LessonAssignment> assignments = lessonAssignmentRepository.findAllById(ids);
        if (assignments.size() != ids.size()) {
            throw new IllegalArgumentException("Some assignmentIds do not exist");
        }

        Long bucketUserId = req.userId(); // null => group-wide bucket
        for (LessonAssignment a : assignments) {
            if (!a.getGroup().getId().equals(groupId)) {
                throw new IllegalArgumentException("All assignments must belong to the same group");
            }
            Long aUserId = (a.getAssignedToUser() == null) ? null : a.getAssignedToUser().getId();
            if (!Objects.equals(bucketUserId, aUserId)) {
                throw new IllegalArgumentException("All assignments must belong to the same bucket (group-wide or the same user)");
            }
        }

        Map<Long, LessonAssignment> byId = new HashMap<>();
        for (LessonAssignment a : assignments) byId.put(a.getId(), a);

        long order = 0L;
        for (Long id : ids) {
            LessonAssignment a = byId.get(id);
            a.setDisplayOrder(order++);
        }

        lessonAssignmentRepository.saveAll(assignments);
    }

    @Transactional
    public void archive(String actorEmail, Long lessonId) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));

        if (!lesson.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        if (!lesson.isArchived()) {
            lesson.archive();
        }
    }

    private List<LessonItemResponse> loadItems(Long lessonId) {
        return lessonItemRepository.findByLessonIdOrderByPosition(lessonId)
                .stream()
                .map(li -> new LessonItemResponse(li.getPosition(), li.getItemType(), li.getTask() == null ? null : li.getTask().getId()))
                .toList();
    }

    private LessonResponse toLessonResponse(Lesson lesson, List<LessonItemResponse> items) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getStatus(),
                items,
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    private LessonAssignmentResponse toAssignmentResponse(LessonAssignment a) {
        return new LessonAssignmentResponse(
                a.getId(),
                a.getGroup().getId(),
                a.getLesson().getId(),
                a.getLesson().getTitle(),
                a.getLesson().getStatus(),
                a.getAssignedToUser() == null ? null : a.getAssignedToUser().getId(),
                a.getDisplayOrder(),
                a.getVisibleFrom(),
                a.getVisibleTo(),
                a.getCreatedAt()
        );
    }

    private User requireActor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private String normalizeNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}

