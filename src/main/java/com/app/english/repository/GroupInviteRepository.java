package com.app.english.repository;

import com.app.english.models.GroupInvite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    Optional<GroupInvite> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GroupInvite> findOneByTokenHash(String tokenHash);

    List<GroupInvite> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Optional<GroupInvite> findByIdAndGroupId(Long id, Long groupId);

    @Query("""
       select count(i)
       from GroupInvite i
       where i.group.id = :groupId
         and i.revoked = false
         and i.expiresAt > :now
         and (i.maxUses is null or i.usedCount < i.maxUses)
    """)
    long countActiveInvites(Long groupId, Instant now);

    @Query("""
       select i
       from GroupInvite i
       where i.group.id = :groupId
         and i.id <> :excludeInviteId
         and (
              i.revoked = true
              or i.expiresAt <= :now
              or (i.maxUses is not null and i.usedCount >= i.maxUses)
         )
       order by i.createdAt asc
    """)
    List<GroupInvite> findPurgeCandidatesExcluding(Long groupId, Instant now, Long excludeInviteId);

}
