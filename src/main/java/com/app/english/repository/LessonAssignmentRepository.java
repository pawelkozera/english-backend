package com.app.english.repository;

import com.app.english.dto.lessons.LessonAssignmentResponse;
import com.app.english.models.LessonAssignment;
import com.app.english.models.LessonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface LessonAssignmentRepository extends JpaRepository<LessonAssignment, Long> {

    @Query("""
        select min(a.displayOrder) from LessonAssignment a
        where a.group.id = :groupId
          and (
            (:userId is null and a.assignedToUser is null)
            or (:userId is not null and a.assignedToUser.id = :userId)
          )
    """)
    Long findMinDisplayOrder(Long groupId, Long userId);

    // -------------------------
    // TEACHER view (single group)
    // -------------------------

    @Query("""
        select new com.app.english.dto.lessons.LessonAssignmentResponse(
          a.id, g.id, l.id, l.title, l.status,
          null, a.displayOrder, a.visibleFrom, a.visibleTo, a.createdAt
        )
        from LessonAssignment a
        join a.group g
        join a.lesson l
        where g.id = :groupId
          and a.assignedToUser is null
        order by a.displayOrder asc, a.createdAt desc
    """)
    Page<LessonAssignmentResponse> pageTeacherGroupWide(Long groupId, Pageable pageable);

    @Query("""
        select new com.app.english.dto.lessons.LessonAssignmentResponse(
          a.id, g.id, l.id, l.title, l.status,
          u.id, a.displayOrder, a.visibleFrom, a.visibleTo, a.createdAt
        )
        from LessonAssignment a
        join a.group g
        join a.lesson l
        join a.assignedToUser u
        where g.id = :groupId
          and u.id = :userId
        order by a.displayOrder asc, a.createdAt desc
    """)
    Page<LessonAssignmentResponse> pageTeacherPerUser(Long groupId, Long userId, Pageable pageable);

    // -------------------------
    // STUDENT view (across my groups)
    // -------------------------

    @Query("""
        select new com.app.english.dto.lessons.LessonAssignmentResponse(
          a.id, g.id, l.id, l.title, l.status,
          null, a.displayOrder, a.visibleFrom, a.visibleTo, a.createdAt
        )
        from LessonAssignment a
        join a.group g
        join a.lesson l
        where g.id in :groupIds
          and a.assignedToUser is null
          and (a.visibleFrom is null or a.visibleFrom <= :now)
          and (a.visibleTo is null or a.visibleTo >= :now)
          and l.status <> :archived
        order by g.id asc, a.displayOrder asc, a.createdAt desc
    """)
    Page<LessonAssignmentResponse> pageStudentGroupWide(List<Long> groupIds, Instant now, LessonStatus archived, Pageable pageable);

    @Query("""
        select new com.app.english.dto.lessons.LessonAssignmentResponse(
          a.id, g.id, l.id, l.title, l.status,
          u.id, a.displayOrder, a.visibleFrom, a.visibleTo, a.createdAt
        )
        from LessonAssignment a
        join a.group g
        join a.lesson l
        join a.assignedToUser u
        where u.id = :userId
          and (a.visibleFrom is null or a.visibleFrom <= :now)
          and (a.visibleTo is null or a.visibleTo >= :now)
          and l.status <> :archived
        order by g.id asc, a.displayOrder asc, a.createdAt desc
    """)
    Page<LessonAssignmentResponse> pageStudentPersonal(Long userId, Instant now, LessonStatus archived, Pageable pageable);

    @Query("""
        select a from LessonAssignment a
        join fetch a.lesson l
        join fetch a.group g
        left join fetch a.assignedToUser u
        where a.group.id = :groupId
          and (
            (:userId is null and a.assignedToUser is null)
            or (:userId is not null and u.id = :userId)
          )
        order by a.displayOrder asc, a.createdAt desc
    """)
    List<LessonAssignment> findForGroup(Long groupId, Long userId);

    @Query("""
        select (count(a) > 0) from LessonAssignment a
        where a.group.id = :groupId
          and a.lesson.id = :lessonId
          and a.assignedToUser is null
    """)
    boolean existsGroupWide(Long groupId, Long lessonId);

    @Query("""
        select (count(a) > 0) from LessonAssignment a
        where a.group.id = :groupId
          and a.lesson.id = :lessonId
          and a.assignedToUser.id = :userId
    """)
    boolean existsForUser(Long groupId, Long lessonId, Long userId);

    Optional<LessonAssignment> findByIdAndGroupId(Long id, Long groupId);

    @Query("""
        select a.lesson.id, a.id from LessonAssignment a
        where a.group.id = :groupId
          and a.assignedToUser is null
          and a.lesson.id in :lessonIds
    """)
    List<Object[]> findExistingGroupWide(Long groupId, Collection<Long> lessonIds);

    @Query("""
        select a.lesson.id, a.id from LessonAssignment a
        where a.group.id = :groupId
          and a.assignedToUser.id = :userId
          and a.lesson.id in :lessonIds
    """)
    List<Object[]> findExistingForUser(Long groupId, Long userId, Collection<Long> lessonIds);

    @Query("""
        select a from LessonAssignment a
        join fetch a.lesson l
        join fetch a.group g
        left join fetch a.assignedToUser u
        join fetch a.assignedBy ab
        where a.id = :assignmentId
    """)
    Optional<LessonAssignment> findByIdWithLessonAndGroup(Long assignmentId);
}