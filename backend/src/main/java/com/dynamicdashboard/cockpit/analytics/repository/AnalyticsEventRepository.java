package com.dynamicdashboard.cockpit.analytics.repository;

import com.dynamicdashboard.cockpit.analytics.domain.AnalyticsEventEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AnalyticsAction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, UUID> {

    List<AnalyticsEventEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);

    List<AnalyticsEventEntity> findByActionOrderByOccurredAtDesc(AnalyticsAction action, Pageable pageable);

    List<AnalyticsEventEntity> findByOccurredAtAfterOrderByOccurredAtDesc(Instant threshold);

    @Modifying
    @Query("update AnalyticsEventEntity e set e.actorUser = null where e.actorUser.id = :userId")
    void nullifyActorByUserId(@Param("userId") UUID userId);
}