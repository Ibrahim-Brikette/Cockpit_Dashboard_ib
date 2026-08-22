import { UserAccountDto } from '../dtos/user.dto';
import { UserProfile } from '@core/services/user.service';
export class UserMapper {
  static toDomain(dto: UserAccountDto): UserProfile {
    const names = (dto.displayName || dto.username).trim().split(' ');
    const initials = names.map((n: string) => n[0]).join('').toUpperCase().slice(0, 2);
    return {
      id:            dto.id,
      username:      dto.username,
      email:         dto.email,
      displayName:   dto.displayName || dto.username,
      initials:      initials || '??',
      accountStatus: dto.accountStatus,
      lastLoginAt:   dto.lastLoginAt
    };
  }
}
