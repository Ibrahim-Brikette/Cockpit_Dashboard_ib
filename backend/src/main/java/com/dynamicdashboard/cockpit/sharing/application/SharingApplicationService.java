package com.dynamicdashboard.cockpit.sharing.application;

import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardRepository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserGroupEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.identity.repository.UserGroupRepository;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.sharing.application.dto.CreateShareGrantRequest;
import com.dynamicdashboard.cockpit.sharing.application.dto.ShareGrantDto;
import com.dynamicdashboard.cockpit.sharing.domain.DashboardShareGrantEntity;
import com.dynamicdashboard.cockpit.sharing.domain.QueryShareGrantEntity;
import com.dynamicdashboard.cockpit.sharing.repository.DashboardShareGrantRepository;
import com.dynamicdashboard.cockpit.sharing.repository.QueryShareGrantRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.ShareLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Share grant management — works in both STANDALONE and INTEGRATED modes.
 * Resource ownership (dashboard/query) is validated here; caller authorisation
 * (only resource owner or admin may manage grants) is enforced in the controller
 * via @PreAuthorize.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharingApplicationService {

    private final DashboardShareGrantRepository dashboardShareGrantRepository;
    private final QueryShareGrantRepository     queryShareGrantRepository;
    private final DashboardRepository           dashboardRepository;
    private final DataQueryRepository           dataQueryRepository;
    private final UserAccountRepository         userAccountRepository;
    private final UserGroupRepository           userGroupRepository;

    // =========================================================================
    // DASHBOARD GRANTS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ShareGrantDto> getDashboardGrants(UUID dashboardId) {
        return dashboardShareGrantRepository.findByDashboard_Id(dashboardId).stream()
                .map(this::toDashboardGrantDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShareGrantDto createDashboardGrant(UUID dashboardId, CreateShareGrantRequest request) {
        DashboardEntity dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new NoSuchElementException("Dashboard not found: " + dashboardId));

        DashboardShareGrantEntity grant = new DashboardShareGrantEntity();
        grant.setDashboard(dashboard);
        grant.setShareLevel(request.shareLevel());
        grant.setAccessLevel(request.accessLevel());

        resolveGrantee(request, grant);

        dashboardShareGrantRepository.save(grant);
        log.info("Dashboard grant created: dashboardId={} shareLevel={} accessLevel={}",
                dashboardId, request.shareLevel(), request.accessLevel());
        return toDashboardGrantDto(grant);
    }

    @Transactional
    public void removeDashboardGrant(UUID grantId) {
        if (!dashboardShareGrantRepository.existsById(grantId)) {
            throw new NoSuchElementException("Dashboard share grant not found: " + grantId);
        }
        dashboardShareGrantRepository.deleteById(grantId);
        log.info("Dashboard grant removed: grantId={}", grantId);
    }

    // =========================================================================
    // QUERY GRANTS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ShareGrantDto> getQueryGrants(UUID queryId) {
        return queryShareGrantRepository.findByQuery_Id(queryId).stream()
                .map(this::toQueryGrantDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShareGrantDto createQueryGrant(UUID queryId, CreateShareGrantRequest request) {
        DataQueryEntity query = dataQueryRepository.findById(queryId)
                .orElseThrow(() -> new NoSuchElementException("Query not found: " + queryId));

        QueryShareGrantEntity grant = new QueryShareGrantEntity();
        grant.setQuery(query);
        grant.setShareLevel(request.shareLevel());
        grant.setAccessLevel(request.accessLevel());

        // Query grants support USERS and GROUP levels (no ROLE column on entity)
        if (request.shareLevel() == ShareLevel.USERS && request.granteeUserId() != null) {
            UserAccountEntity user = userAccountRepository.findById(request.granteeUserId())
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + request.granteeUserId()));
            grant.setGranteeUser(user);
        } else if (request.shareLevel() == ShareLevel.GROUP && request.granteeGroupId() != null) {
            UserGroupEntity group = userGroupRepository.findById(request.granteeGroupId())
                    .orElseThrow(() -> new NoSuchElementException("Group not found: " + request.granteeGroupId()));
            grant.setGranteeGroup(group);
        }

        queryShareGrantRepository.save(grant);
        log.info("Query grant created: queryId={} shareLevel={} accessLevel={}",
                queryId, request.shareLevel(), request.accessLevel());
        return toQueryGrantDto(grant);
    }

    @Transactional
    public void removeQueryGrant(UUID grantId) {
        if (!queryShareGrantRepository.existsById(grantId)) {
            throw new NoSuchElementException("Query share grant not found: " + grantId);
        }
        queryShareGrantRepository.deleteById(grantId);
        log.info("Query grant removed: grantId={}", grantId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves granteeUser or granteeGroup on a dashboard grant based on shareLevel.
     * PRIVATE and ORGANIZATION grants have no specific grantee row — the shareLevel itself carries the meaning.
     */
    private void resolveGrantee(CreateShareGrantRequest request, DashboardShareGrantEntity grant) {
        if (request.shareLevel() == ShareLevel.USERS && request.granteeUserId() != null) {
            UserAccountEntity user = userAccountRepository.findById(request.granteeUserId())
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + request.granteeUserId()));
            grant.setGranteeUser(user);
        } else if (request.shareLevel() == ShareLevel.GROUP && request.granteeGroupId() != null) {
            UserGroupEntity group = userGroupRepository.findById(request.granteeGroupId())
                    .orElseThrow(() -> new NoSuchElementException("Group not found: " + request.granteeGroupId()));
            grant.setGranteeGroup(group);
        }
        // PRIVATE / ORGANIZATION — no grantee FK needed, shareLevel is sufficient
    }

    private ShareGrantDto toDashboardGrantDto(DashboardShareGrantEntity g) {
        return new ShareGrantDto(
                g.getId(),
                g.getShareLevel().name(),
                g.getAccessLevel().name(),
                g.getGranteeUser()  != null ? g.getGranteeUser().getId()   : null,
                g.getGranteeGroup() != null ? g.getGranteeGroup().getId()  : null,
                resolveGranteeName(g.getShareLevel(),
                        g.getGranteeUser(), g.getGranteeGroup()));
    }

    private ShareGrantDto toQueryGrantDto(QueryShareGrantEntity g) {
        return new ShareGrantDto(
                g.getId(),
                g.getShareLevel().name(),
                g.getAccessLevel().name(),
                g.getGranteeUser()  != null ? g.getGranteeUser().getId()   : null,
                g.getGranteeGroup() != null ? g.getGranteeGroup().getId()  : null,
                resolveGranteeName(g.getShareLevel(),
                        g.getGranteeUser(), g.getGranteeGroup()));
    }

    private String resolveGranteeName(ShareLevel level,
                                      UserAccountEntity user,
                                      UserGroupEntity group) {
        return switch (level) {
            case USERS        -> user  != null ? user.getDisplayName()  : null;
            case GROUP        -> group != null ? group.getGroupName()   : null;
            case ORGANIZATION -> "Organisation";
            case PRIVATE      -> null;
        };
    }
}
