package com.app.english.repository;

import com.app.english.models.GroupInviteUse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInviteUseRepository extends JpaRepository<GroupInviteUse, Long> {
    boolean existsByInviteIdAndUserId(Long inviteId, Long userId);
}
