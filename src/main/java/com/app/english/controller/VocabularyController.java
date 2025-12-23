package com.app.english.controller;

import com.app.english.dto.vocabulary.CreateVocabularyRequest;
import com.app.english.dto.vocabulary.UpdateVocabularyRequest;
import com.app.english.dto.vocabulary.VocabularyResponse;
import com.app.english.service.VocabularyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    // Creates a new vocabulary entry in the user's private library.
    @PostMapping
    public VocabularyResponse create(@Valid @RequestBody CreateVocabularyRequest request, Authentication auth) {
        return vocabularyService.create(auth.getName(), request);
    }

    // Lists/searches vocabulary entries in the user's private library.
    @GetMapping
    public Page<VocabularyResponse> search(@RequestParam(required = false) String q, Pageable pageable, Authentication auth) {
        return vocabularyService.searchMine(auth.getName(), q, pageable);
    }

    @GetMapping("/{id}")
    public VocabularyResponse get(@PathVariable Long id, Authentication auth) {
        return vocabularyService.get(auth.getName(), id);
    }

    @PutMapping("/{id}")
    public VocabularyResponse update(@PathVariable Long id, @Valid @RequestBody UpdateVocabularyRequest request, Authentication auth) {
        return vocabularyService.update(auth.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        vocabularyService.delete(auth.getName(), id);
    }
}
