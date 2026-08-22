package com.dynamicdashboard.cockpit.identity.application;

import com.dynamicdashboard.cockpit.analytics.repository.AnalyticsEventRepository;
import com.dynamicdashboard.cockpit.audit.repository.AuditEventRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardRepository;
import com.dynamicdashboard.cockpit.identity.application.dto.*;
import com.dynamicdashboard.cockpit.identity.application.mapper.IdentityMapper;
import com.dynamicdashboard.cockpit.identity.domain.*;
import com.dynamicdashboard.cockpit.identity.repository.*;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AssignmentScope;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.MembershipRole;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import com.dynamicdashboard.cockpit.shared.security.MailService;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.EmailVerificationTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.EmailVerificationTokenRepository;
import com.dynamicdashboard.cockpit.sharing.repository.DashboardShareGrantRepository;
import com.dynamicdashboard.cockpit.sharing.repository.QueryShareGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityApplicationService {

    /** Verification link TTL: 24h — longer than password reset (5 min) because this is account setup, not an emergency. */
    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository            userAccountRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final IdentityMapper                   identityMapper;
    private final CurrentUserService               currentUserService;
    private final PasswordEncoder                  passwordEncoder;
    private final MailService                      mailService;

    // Phase 3 — role / group management
    private final RoleRepository                   roleRepository;
    private final UserRoleAssignmentRepository     userRoleAssignmentRepository;
    private final UserGroupRepository              userGroupRepository;
    private final UserGroupMembershipRepository    userGroupMembershipRepository;

    // Needed for safe user deletion — clean FK references before removing the row
    private final DashboardRepository              dashboardRepository;
    private final DataQueryRepository              dataQueryRepository;
    private final DashboardShareGrantRepository    dashboardShareGrantRepository;
    private final QueryShareGrantRepository        queryShareGrantRepository;
    private final AuditEventRepository             auditEventRepository;
    private final AnalyticsEventRepository         analyticsEventRepository;

    // -------------------------------------------------------------------------
    // OLD getCurrentUser — commented, not deleted
    // -------------------------------------------------------------------------
    // Was hardcoded to look up "ahaddad" regardless of who was authenticated.
    // If ahaddad did not exist it created a fake user row with a truncated
    // password hash — completely disconnected from the real authenticated user.
    // Removed because /me must return the profile of whoever holds the JWT,
    // not a fixed dev placeholder. Login is the only entry point — there is
    // no concept of a "default user" when unauthenticated.
    //
    // @Transactional
    // public UserAccountDto getCurrentUser() {
    //     UserAccountEntity user = userAccountRepository.findByUsername("ahaddad")
    //             .orElseGet(() -> {
    //                 UserAccountEntity newUser = new UserAccountEntity();
    //                 newUser.setUsername("ahaddad");
    //                 newUser.setEmail("amine.haddad@prestacode.com");
    //                 newUser.setDisplayName("Amine Haddad");
    //                 newUser.setPasswordHash("$2a$10$7Q9b9K...");
    //                 newUser.setAccountStatus(AccountStatus.ACTIVE);
    //                 newUser.setLastLoginAt(Instant.now());
    //                 return userAccountRepository.save(newUser);
    //             });
    //     return identityMapper.toDto(user);
    // }
    // -------------------------------------------------------------------------

    /**
     * Returns the profile of the currently authenticated user.
     * Reads sub (userId UUID) from the JWT already validated by Spring Security,
     * then fetches the full row from the DB.
     * No fallback — if there is no authenticated user the request never reaches here
     * because Spring rejects it at the filter level before any service runs.
     */
    @Transactional(readOnly = true)
    public UserAccountDto getCurrentUser() {
        return identityMapper.toDto(currentUserService.getCurrentUser());
    }

    /**
     * Returns the profile of the user identified by id.
     * Security rule (enforced in the controller via @PreAuthorize):
     *   - the authenticated user is fetching their own profile, OR
     *   - the authenticated user has ROLE_TENANT_ADMIN or ROLE_SYSTEM_ADMIN
     */
    @Transactional(readOnly = true)
    public UserAccountDto getUserById(UUID id) {
        return userAccountRepository.findById(id)
                .map(identityMapper::toDto)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserAccountDto> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .map(identityMapper::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // STANDALONE-only — user creation with email verification
    // Called by IdentityController which guards this behind @ConditionalOnProperty
    // =========================================================================

    /**
     * Creates a new user account and sends an activation email.
     *
     * The admin provides username, email, displayName, and tenantId.
     * No password is set at creation time — the user chooses their own password
     * when they click the verification link.
     *
     * Steps:
     *   1. Validate uniqueness of username and email.
     *   2. Persist the user with status=PENDING and a placeholder password hash
     *      (a BCrypt hash of a random UUID — valid format, matches no known input).
     *   3. Generate a 32-byte cryptographically random token, store its SHA-256 hash.
     *   4. Send the activation email with the raw token in the link.
     *
     * The placeholder password prevents Spring Security from throwing
     * IllegalArgumentException if the account is somehow reached at login
     * before verification (it will simply fail the password check and return 401).
     *
     * @throws IllegalArgumentException if username or email is already taken
     */
    @Transactional
    public UserAccountDto createUser(CreateUserRequest request) {

        // 1. Uniqueness checks — fail fast before writing anything
        if (userAccountRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken: " + request.username());
        }
        if (userAccountRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered: " + request.email());
        }

        // 2. Create the user — PENDING status means they cannot log in yet
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setTenantId(request.tenantId());
        user.setAccountStatus(AccountStatus.PENDING);

        // Placeholder password: BCrypt hash of a random UUID that is immediately discarded.
        // It is a syntactically valid BCrypt hash so Spring Security won't throw on load,
        // but it matches no known input — the account cannot be used until verification.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        userAccountRepository.save(user);

        // 3. Generate verification token — raw value leaves only via the email link
        String rawToken = generateRawToken();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setTokenHash(sha256(rawToken));
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        token.setUsed(false);
        emailVerificationTokenRepository.save(token);

        // DEBUG — remove before going to production
        // Lets you test POST /api/auth/verify-email in Postman without a real mail server
        log.debug("[DEV] Verification token for userId={}: {}", user.getId(), rawToken);

        // 4. Send the activation email — failure is logged, never rolls back the transaction
        mailService.sendVerificationEmail(user.getEmail(), user.getDisplayName(), rawToken);

        log.info("User created (PENDING): userId={} username={}", user.getId(), user.getUsername());
        return identityMapper.toDto(user);
    }

    // =========================================================================
    // ROLES
    // =========================================================================

    @Transactional(readOnly = true)
    public List<RoleDto> getRoles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleDto(r.getId(), r.getRoleName(), r.getRoleDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserRoleDto> getUserRoles(UUID userId) {
        return userRoleAssignmentRepository.findByUser_Id(userId).stream()
                .map(a -> new UserRoleDto(
                        a.getId(),
                        a.getRole().getId(),
                        a.getRole().getRoleName(),
                        a.getAssignmentScope().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserRoleDto assignRole(UUID userId, AssignRoleRequest request) {
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        RoleEntity role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + request.roleId()));

        // Idempotent guard — prevent duplicate assignment
        userRoleAssignmentRepository.findByUser_IdAndRole_Id(userId, request.roleId())
                .ifPresent(e -> { throw new IllegalArgumentException("Role already assigned to this user"); });

        UserRoleAssignmentEntity assignment = new UserRoleAssignmentEntity();
        assignment.setUser(user);
        assignment.setRole(role);
        assignment.setAssignmentScope(
                request.assignmentScope() != null ? request.assignmentScope() : AssignmentScope.ORGANIZATION);
        userRoleAssignmentRepository.save(assignment);

        log.info("Role assigned: userId={} roleId={} scope={}", userId, request.roleId(), assignment.getAssignmentScope());
        return new UserRoleDto(assignment.getId(), role.getId(), role.getRoleName(), assignment.getAssignmentScope().name());
    }

    @Transactional
    public void removeRole(UUID userId, UUID roleId) {
        userRoleAssignmentRepository.findByUser_IdAndRole_Id(userId, roleId)
                .orElseThrow(() -> new NoSuchElementException("Role assignment not found for userId=" + userId + " roleId=" + roleId));
        userRoleAssignmentRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Role removed: userId={} roleId={}", userId, roleId);
    }

    // =========================================================================
    // GROUPS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<GroupDto> getGroups() {
        return userGroupRepository.findAll().stream()
                .map(g -> new GroupDto(g.getId(), g.getGroupName(), g.getGroupCode(), g.getGroupDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupDto createGroup(CreateGroupRequest request) {
        if (userGroupRepository.findByGroupCode(request.groupCode()).isPresent()) {
            throw new IllegalArgumentException("Group code already exists: " + request.groupCode());
        }
        UserGroupEntity group = new UserGroupEntity();
        group.setGroupName(request.groupName());
        group.setGroupCode(request.groupCode());
        group.setGroupDescription(request.groupDescription());
        userGroupRepository.save(group);
        log.info("Group created: groupId={} code={}", group.getId(), group.getGroupCode());
        return new GroupDto(group.getId(), group.getGroupName(), group.getGroupCode(), group.getGroupDescription());
    }

    @Transactional(readOnly = true)
    public List<GroupMemberDto> getGroupMembers(UUID groupId) {
        return userGroupMembershipRepository.findByGroup_Id(groupId).stream()
                .map(m -> new GroupMemberDto(
                        m.getId(),
                        m.getUser().getId(),
                        m.getUser().getDisplayName(),
                        m.getUser().getUsername(),
                        m.getMembershipRole().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupMemberDto addGroupMember(UUID groupId, AddGroupMemberRequest request) {
        UserGroupEntity group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));
        UserAccountEntity user = userAccountRepository.findById(request.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + request.userId()));

        // Idempotent guard — prevent duplicate membership
        userGroupMembershipRepository.findByGroup_IdAndUser_Id(groupId, request.userId())
                .ifPresent(e -> { throw new IllegalArgumentException("User is already a member of this group"); });

        UserGroupMembershipEntity membership = new UserGroupMembershipEntity();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setMembershipRole(
                request.membershipRole() != null ? request.membershipRole() : MembershipRole.MEMBER);
        userGroupMembershipRepository.save(membership);

        log.info("Group member added: groupId={} userId={} role={}", groupId, request.userId(), membership.getMembershipRole());
        return new GroupMemberDto(
                membership.getId(),
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                membership.getMembershipRole().name());
    }

    /**
     * User-centric group view — "which groups does this user belong to?"
     * Used by the detail panel Groupes tab in the admin UI.
     */
    @Transactional(readOnly = true)
    public List<UserGroupDto> getUserGroups(UUID userId) {
        return userGroupMembershipRepository.findByUser_Id(userId).stream()
                .map(m -> new UserGroupDto(
                        m.getId(),
                        m.getGroup().getId(),
                        m.getGroup().getGroupName(),
                        m.getGroup().getGroupCode(),
                        m.getMembershipRole().name()))
                .collect(Collectors.toList());
    }

    /**
     * Permanently deletes a group after cleaning up all FK references.
     *
     * The DB has ON DELETE CASCADE on user_group_membership.group_id,
     * dashboard_share_grant.grantee_group_id, and query_share_grant.grantee_group_id,
     * so the DB would cascade automatically. We still run the JPQL bulk deletes first
     * to keep the Hibernate persistence context consistent before the entity delete.
     * Both layers (Hibernate + DB) must agree on what is gone before the commit.
     *
     * Cleanup order:
     *   1. All memberships pointing to this group (JPQL bulk delete + DB cascade).
     *   2. All dashboard share grants where this group is the grantee.
     *   3. All query share grants where this group is the grantee.
     *   4. The group row itself.
     *
     * User accounts are never touched — members simply lose this group's access.
     *
     * @throws NoSuchElementException if the group does not exist.
     */
    @Transactional
    public void deleteGroup(UUID groupId) {
        UserGroupEntity group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));

        // Clean dependent rows explicitly so the Hibernate session stays consistent.
        // clearAutomatically = true on each @Modifying method evicts stale entities
        // from the first-level cache after each bulk delete.
        userGroupMembershipRepository.deleteByGroup_Id(groupId);
        dashboardShareGrantRepository.deleteByGranteeGroup_Id(groupId);
        queryShareGrantRepository.deleteByGranteeGroup_Id(groupId);

        userGroupRepository.delete(group);
        log.info("Group deleted: groupId={} code={}", groupId, group.getGroupCode());
    }

    @Transactional
    public void removeGroupMember(UUID groupId, UUID userId) {
        userGroupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new NoSuchElementException("Membership not found for groupId=" + groupId + " userId=" + userId));
        userGroupMembershipRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("Group member removed: groupId={} userId={}", groupId, userId);
    }

    // =========================================================================
    // DELETE USER — STANDALONE only
    // =========================================================================

    /**
     * Permanently deletes a user account after cleaning up all FK references.
     *
     * Self-deletion is blocked: an admin cannot delete their own account through
     * the UI — this prevents accidental lockout.
     *
     * Deletion is also blocked if the user owns dashboards or queries.  Those
     * resources must be reassigned or deleted before the user can be removed —
     * we do not cascade-delete business data silently.
     *
     * Everything else is cleaned automatically in the same transaction:
     *   - role assignments (NOT NULL FK → must delete)
     *   - group memberships (NOT NULL FK → must delete)
     *   - share grants where this user is the grantee (nullable FK → delete)
     *   - audit events where this user is the actor (nullable FK → nullify)
     *   - analytics events where this user is the actor (nullable FK → nullify)
     *
     * @throws IllegalArgumentException      if the caller is trying to delete themselves
     * @throws NoSuchElementException        if the target user does not exist
     * @throws UserDeletionBlockedException  if the user owns dashboards or queries
     */
    @Transactional
    public void deleteUser(UUID id) {
        UserAccountEntity current = currentUserService.getCurrentUser();
        if (current != null && current.getId().equals(id)) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }
        UserAccountEntity user = userAccountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // Block if the user owns business data — do not silently cascade-delete dashboards or queries
        long ownedDashboards = dashboardRepository.countByOwnerId(id);
        long ownedQueries    = dataQueryRepository.countByOwnerId(id);
        if (ownedDashboards > 0 || ownedQueries > 0) {
            throw new UserDeletionBlockedException(ownedDashboards, ownedQueries);
        }

        // Clean NOT-NULL FK references (must delete rows, not nullify)
        userRoleAssignmentRepository.deleteByUser_Id(id);
        userGroupMembershipRepository.deleteByUser_Id(id);

        // Clean nullable FK references where this user is the grantee
        dashboardShareGrantRepository.deleteByGranteeUser_Id(id);
        queryShareGrantRepository.deleteByGranteeUser_Id(id);

        // Nullify nullable actor references — preserve the log entries, just anonymise the actor
        auditEventRepository.nullifyActorByUserId(id);
        analyticsEventRepository.nullifyActorByUserId(id);

        userAccountRepository.delete(user);
        log.info("User deleted: userId={} username={}", id, user.getUsername());
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** 32 bytes from SecureRandom → Base64url without padding → 43-char opaque token. */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
