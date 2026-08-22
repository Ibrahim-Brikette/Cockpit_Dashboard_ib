package com.dynamicdashboard.cockpit.audit.repository;
import com.dynamicdashboard.cockpit.audit.domain.AuditEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    List<AuditEventEntity> findTop200ByOccurredAtAfterOrderByOccurredAtDesc(Instant threshold);
    List<AuditEventEntity> findAllByOrderByOccurredAtDesc();

    @Modifying
    @Query("update AuditEventEntity e set e.actorUser = null where e.actorUser.id = :userId")
    void nullifyActorByUserId(@Param("userId") UUID userId);
}
