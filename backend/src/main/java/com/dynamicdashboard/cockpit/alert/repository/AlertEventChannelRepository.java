package com.dynamicdashboard.cockpit.alert.repository;

import com.dynamicdashboard.cockpit.alert.domain.AlertEventChannelEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertEventChannelEntity.AlertEventChannelId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertEventChannelRepository extends JpaRepository<AlertEventChannelEntity, AlertEventChannelId> {

    List<AlertEventChannelEntity> findByIdEventId(UUID eventId);
}