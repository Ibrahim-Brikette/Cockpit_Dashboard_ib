package com.dynamicdashboard.cockpit.sharing.repository;

import com.dynamicdashboard.cockpit.sharing.domain.QueryShareGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface QueryShareGrantRepository extends JpaRepository<QueryShareGrantEntity, UUID> {
    @Query("""
    select max(
        case sg.accessLevel
            when 'OWNER' then 3
            when 'EDIT' then 2
            when 'READ' then 1
            else 0
        end
    )
    from QueryShareGrantEntity sg
    where sg.query.id = :queryId
      and (
           (sg.shareLevel = 'USERS' and sg.granteeUser.id = :userId)
        or (sg.shareLevel = 'GROUP' and sg.granteeGroup.id in (
                select m.group.id from UserGroupMembershipEntity m where m.user.id = :userId
            ))
      )
    """)
    Integer findMaxAccessLevelRank(@Param("queryId") UUID queryId, @Param("userId") UUID userId);
}
