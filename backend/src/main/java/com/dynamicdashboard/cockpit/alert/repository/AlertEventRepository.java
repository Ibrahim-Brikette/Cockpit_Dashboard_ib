package com.dynamicdashboard.cockpit.alert.repository;

import com.dynamicdashboard.cockpit.alert.domain.AlertEventEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertEventRepository extends JpaRepository<AlertEventEntity, UUID> {

    List<AlertEventEntity> findAllByOrderByTriggeredAtDesc();

    Optional<AlertEventEntity> findFirstByRuleIdAndStatusNotOrderByTriggeredAtDesc(UUID ruleId, AlertStatus status);
}