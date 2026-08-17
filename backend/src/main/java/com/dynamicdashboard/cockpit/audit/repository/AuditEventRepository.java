package com.dynamicdashboard.cockpit.audit.repository;
import com.dynamicdashboard.cockpit.audit.domain.AuditEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    List<AuditEventEntity> findTop200ByOccurredAtAfterOrderByOccurredAtDesc(Instant threshold);
    List<AuditEventEntity> findAllByOrderByOccurredAtDesc();
}
