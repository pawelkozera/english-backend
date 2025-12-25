package com.app.english.repository;

import com.app.english.models.TaskVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskVocabularyRepository extends JpaRepository<TaskVocabulary, Long> {

    @Query("""
        select tv.vocabulary.id
        from TaskVocabulary tv
        where tv.task.id = :taskId
        order by tv.position asc
    """)
    List<Long> findVocabularyIds(Long taskId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TaskVocabulary tv where tv.task.id = :taskId")
    int deleteByTaskId(Long taskId);
}
