package com.app.english.service;

import com.app.english.dto.vocabulary.CreateVocabularyRequest;
import com.app.english.dto.vocabulary.UpdateVocabularyRequest;
import com.app.english.dto.vocabulary.VocabularyResponse;
import com.app.english.exceptions.ForbiddenException;
import com.app.english.exceptions.VocabularyNotFoundException;
import com.app.english.models.User;
import com.app.english.models.Vocabulary;
import com.app.english.repository.UserRepository;
import com.app.english.repository.VocabularyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository, UserRepository userRepository) {
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VocabularyResponse create(String actorEmail, CreateVocabularyRequest req) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Vocabulary v = new Vocabulary(
                req.termEn().trim(),
                req.termPl().trim(),
                normalizeNullable(req.exampleEn()),
                normalizeNullable(req.examplePl()),
                req.imageMediaId(),
                req.audioMediaId(),
                actor
        );

        Vocabulary saved = vocabularyRepository.save(v);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VocabularyResponse get(String actorEmail, Long id) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Vocabulary v = vocabularyRepository.findById(id)
                .orElseThrow(() -> new VocabularyNotFoundException("Vocabulary not found"));

        if (!v.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        return toResponse(v);
    }

    @Transactional(readOnly = true)
    public Page<VocabularyResponse> searchMine(String actorEmail, String q, Pageable pageable) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return vocabularyRepository.searchByOwner(actor.getId(), normalizeNullable(q), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public VocabularyResponse update(String actorEmail, Long id, UpdateVocabularyRequest req) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Vocabulary v = vocabularyRepository.findById(id)
                .orElseThrow(() -> new VocabularyNotFoundException("Vocabulary not found"));

        if (!v.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        v.update(
                req.termEn().trim(),
                req.termPl().trim(),
                normalizeNullable(req.exampleEn()),
                normalizeNullable(req.examplePl()),
                req.imageMediaId(),
                req.audioMediaId()
        );

        return toResponse(v);
    }

    @Transactional
    public void delete(String actorEmail, Long id) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Vocabulary v = vocabularyRepository.findById(id)
                .orElseThrow(() -> new VocabularyNotFoundException("Vocabulary not found"));

        if (!v.getCreatedBy().getId().equals(actor.getId())) {
            throw new ForbiddenException("Not allowed");
        }

        vocabularyRepository.delete(v);
    }

    private VocabularyResponse toResponse(Vocabulary v) {
        return new VocabularyResponse(
                v.getId(),
                v.getTermEn(),
                v.getTermPl(),
                v.getExampleEn(),
                v.getExamplePl(),
                v.getImageMediaId(),
                v.getAudioMediaId(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }

    private String normalizeNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
