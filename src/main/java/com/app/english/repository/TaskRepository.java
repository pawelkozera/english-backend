package com.app.english.repository;

import com.app.english.models.Task;
import com.app.english.models.TaskStatus;
import com.app.english.models.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
        select t from Task t
        where t.createdBy.id = :ownerId
          and (:q is null or :q = '' or lower(t.title) like lower(concat('%', :q, '%')))
          and (:type is null or t.type = :type)
          and (:status is null or t.status = :status)
        order by t.updatedAt desc
    """)
    Page<Task> searchByOwner(Long ownerId, String q, TaskType type, TaskStatus status, Pageable pageable);
}
