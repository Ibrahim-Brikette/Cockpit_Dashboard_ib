package com.dynamicdashboard.cockpit.sharing.repository;

import com.dynamicdashboard.cockpit.sharing.domain.DashboardShareGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DashboardShareGrantRepository extends JpaRepository<DashboardShareGrantEntity, UUID> {

    List<DashboardShareGrantEntity> findByDashboard_Id(UUID dashboardId);

    List<DashboardShareGrantEntity> findByGranteeUser_Id(UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from DashboardShareGrantEntity g where g.granteeUser.id = :userId")
    void deleteByGranteeUser_Id(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("delete from DashboardShareGrantEntity g where g.granteeGroup.id = :groupId")
    void deleteByGranteeGroup_Id(@org.springframework.data.repository.query.Param("groupId") UUID groupId);

    @Query("""
        select max(
            case sg.accessLevel
                when 'OWNER' then 3
                when 'EDIT'  then 2
                when 'READ'  then 1
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
          )
    """)
    // Note: ROLE-level sharing is reserved for a future iteration.
    // ShareLevel enum has no ROLE value yet — the granteeRole field on the entity
    // is kept as a placeholder but is not queried here to avoid a dead-clause.
    Integer findMaxAccessLevelRank(@Param("dashboardId") UUID dashboardId, @Param("userId") UUID userId);
}
