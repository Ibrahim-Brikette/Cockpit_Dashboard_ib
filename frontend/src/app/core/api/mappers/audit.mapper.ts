import { AuditEventDto, CreateAuditEventRequestDto } from '../dtos/audit.dto';
import { AuditLogEntry } from '@pages/settings/services/audit.service';
export class AuditMapper {
  static toDomain(dto: AuditEventDto): AuditLogEntry {
    return {
      id: dto.id,
      event: dto.eventType || 'Événement système',
      detail: dto.detailsJson || dto.targetType || 'Détail',
      // [IBR 2026-08-19] Use the actorName resolved by the backend (displayName of the actor).
      // Fall back to 'Système' for system-generated events with no human actor.
      // The old logic overrode 'Système'/'System' with 'Amine Haddad' — that was a placeholder
      // left from early development and has been removed. Backend is the reference: see AuditMapper.java.
      author: dto.actorName || 'Système',
      date: dto.occurredAt ? new Date(dto.occurredAt).toLocaleString('fr-TN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Récemment'
    };
  }
  static toCreateDto(eventType: string, detailsJson: string, targetType: string = 'DASHBOARD', targetId?: string): CreateAuditEventRequestDto {
    return {
      eventType,
      targetType,
      targetId,
      detailsJson,
      sourceIp: '127.0.0.1'
    };
  }
}
