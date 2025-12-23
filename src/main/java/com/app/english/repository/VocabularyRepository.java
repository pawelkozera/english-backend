package com.app.english.repository;

import com.app.english.models.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    @Query("""
        select v from Vocabulary v
        where v.createdBy.id = :ownerId
          and (
            :q is null or :q = '' or
            lower(v.termEn) like lower(concat('%', :q, '%')) or
            lower(v.termPl) like lower(concat('%', :q, '%'))
          )
        order by v.updatedAt desc
    """)
    Page<Vocabulary> searchByOwner(Long ownerId, String q, Pageable pageable);

    boolean existsByImageMediaId(Long mediaId);
    boolean existsByAudioMediaId(Long mediaId);
}
