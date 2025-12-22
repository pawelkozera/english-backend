package com.app.english.repository;

import com.app.english.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByJoinCode(String joinCode);
    boolean existsByJoinCode(String joinCode);
}
