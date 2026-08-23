export interface ShareGrantDto {
  id: string;
  shareLevel: string;       // PRIVATE | USERS | GROUP | ORGANIZATION
  accessLevel: string;      // READ | EDIT | OWNER
  granteeUserId?: string;
  granteeGroupId?: string;
  granteeName?: string;
}

export interface CreateShareGrantRequest {
  shareLevel: string;
  accessLevel: string;
  granteeUserId?: string;
  granteeGroupId?: string;
}
