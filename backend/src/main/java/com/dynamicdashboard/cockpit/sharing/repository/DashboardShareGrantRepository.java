package com.dynamicdashboard.cockpit.sharing.repository;
import com.dynamicdashboard.cockpit.sharing.domain.DashboardShareGrantEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashboardShareGrantRepository extends JpaRepository<DashboardShareGrantEntity, UUID> {
    List<DashboardShareGrantEntity> findByDashboardId(UUID dashboardId);
    @Query("""

            select max(
        case sg.accessLevel
            when 'OWNER' then 3
            when 'EDIT' then 2
            when 'READ' then 1
            else 0
        end
    )
    from DashboardShareGrantEntity sg
    where sg.dashboard.id = :dashboardId
      and (
           (sg.shareLevel = 'USERS' and sg.granteeUser.id = :userId)
        or (sg.shareLevel = 'GROUP' and sg.granteeGroup.id in (
                select m.group.id from UserGroupMembershipEntity m where m.user.id = :userId
            ))
        or (sg.shareLevel = 'ROLE' and sg.granteeRole.id in (
                select ura.role.id from UserRoleAssignmentEntity ura where ura.user.id = :userId
            ))
      )
    """)
    Integer findMaxAccessLevelRank(@Param("queryId") UUID queryId, @Param("userId") UUID userId);
}
