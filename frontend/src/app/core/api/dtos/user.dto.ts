export interface UserAccountDto {
  id?: string;
  username: string;
  email: string;
  displayName?: string;
  accountStatus?: string;
  lastLoginAt?: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  displayName: string;
  tenantId: string;
}

// ── Roles ─────────────────────────────────────────────────────────────────

export interface RoleDto {
  id: string;
  roleName: string;
  roleDescription?: string;
}

export interface UserRoleDto {
  assignmentId: string;
  roleId: string;
  roleName: string;
  assignmentScope: string;
}

export interface AssignRoleRequest {
  roleId: string;
  assignmentScope?: string;
}

// ── Groups ────────────────────────────────────────────────────────────────

export interface GroupDto {
  id: string;
  groupName: string;
  groupCode: string;
  groupDescription?: string;
}

export interface CreateGroupRequest {
  groupName: string;
  groupCode: string;
  groupDescription?: string;
}

export interface GroupMemberDto {
  membershipId: string;
  userId: string;
  displayName?: string;
  username: string;
  membershipRole: string;
}

export interface AddGroupMemberRequest {
  userId: string;
  membershipRole?: string;
}

/** User-centric group view — "which groups does user X belong to?" */
export interface UserGroupDto {
  membershipId: string;
  groupId: string;
  groupName: string;
  groupCode: string;
  membershipRole: string;
}
