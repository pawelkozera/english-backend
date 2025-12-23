package com.app.english.repository;

import com.app.english.models.GroupInviteUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupInviteUseRepository extends JpaRepository<GroupInviteUse, Long> {
    boolean existsByInviteIdAndUserId(Long inviteId, Long userId);

    @Modifying
    @Query("delete from GroupInviteUse u where u.invite.id in :inviteIds")
    int deleteByInviteIds(List<Long> inviteIds);
}
