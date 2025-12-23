package com.app.english.repository;

import com.app.english.models.GroupInvite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    Optional<GroupInvite> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GroupInvite> findOneByTokenHash(String tokenHash);

    List<GroupInvite> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Optional<GroupInvite> findByIdAndGroupId(Long id, Long groupId);
}
