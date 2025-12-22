package com.app.english.repository;

import com.app.english.models.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);

    @Query("""
           select m
           from Membership m
           join fetch m.group g
           where m.user.email = :email
           order by g.createdAt desc
           """)
    List<Membership> findMyMembershipsWithGroups(String email);

    @Query("""
           select m
           from Membership m
           join fetch m.group g
           where m.user.email = :email and g.id = :groupId
           """)
    Optional<Membership> findByUserEmailAndGroupId(String email, Long groupId);

    @Query("""
           select m
           from Membership m
           join fetch m.group g
           join fetch m.user u
           where g.id = :groupId and u.id = :userId
           """)
    Optional<Membership> findByGroupIdAndUserIdFetchAll(Long groupId, Long userId);

    @Query("""
           select m
           from Membership m
           join fetch m.user u
           where m.group.id = :groupId
           order by m.joinedAt asc
           """)
    List<Membership> findMembersByGroupId(Long groupId);
}
