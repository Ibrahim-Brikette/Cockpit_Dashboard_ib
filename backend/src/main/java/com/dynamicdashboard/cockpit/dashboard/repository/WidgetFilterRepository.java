package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetFilterEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WidgetFilterRepository extends JpaRepository<WidgetFilterEntity, UUID> {
    List<WidgetFilterEntity> findByWidgetIdOrderByPositionIndexAsc(UUID widgetId);
    void deleteByWidgetId(UUID widgetId);
}
