package com.dynamicdashboard.cockpit.alert.repository;

import com.dynamicdashboard.cockpit.alert.domain.AlertRuleChannelEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleChannelRepository extends JpaRepository<AlertRuleChannelEntity, UUID> {

    List<AlertRuleChannelEntity> findByRuleId(UUID ruleId);
}