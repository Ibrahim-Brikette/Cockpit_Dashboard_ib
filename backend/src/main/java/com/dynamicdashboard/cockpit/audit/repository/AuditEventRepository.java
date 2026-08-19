package com.dynamicdashboard.cockpit.audit.repository;
import com.dynamicdashboard.cockpit.audit.domain.AuditEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    List<AuditEventEntity> findTop200ByOccurredAtAfterOrderByOccurredAtDesc(Instant threshold);

    /** Conservé pour compatibilité, mais NE PLUS utiliser pour l'affichage courant :
     *  charge toute la table en mémoire. Préférer findTop200ByOccurredAtAfterOrderByOccurredAtDesc. */
    List<AuditEventEntity> findAllByOrderByOccurredAtDesc();

    long countByOccurredAtBefore(Instant threshold);

    @Modifying
    @Query("delete from AuditEventEntity a where a.occurredAt < :threshold")
    void deleteByOccurredAtBefore(@Param("threshold") Instant threshold);

    /** Agrégation faite au niveau base de données (pré-agrégée) au lieu de boucler en Java
     *  sur toute la liste d'événements à chaque affichage du KPI admin. */
    @Query("select a.targetId as targetId, a.detailsJson as label, count(a) as total " +
            "from AuditEventEntity a where a.eventType = :eventType " +
            "group by a.targetId, a.detailsJson order by count(a) desc")
    List<TargetExecutionCount> countGroupedByTarget(@Param("eventType") String eventType, Pageable pageable);

    interface TargetExecutionCount {
        UUID getTargetId();
        String getLabel();
        Long getTotal();
    }
}