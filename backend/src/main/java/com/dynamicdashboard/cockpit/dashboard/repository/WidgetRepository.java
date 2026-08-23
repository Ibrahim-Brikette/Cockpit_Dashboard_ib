package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WidgetRepository extends JpaRepository<WidgetEntity, UUID> {
    List<WidgetEntity> findByDashboardId(UUID dashboardId);
    List<WidgetEntity> findByQueryId(UUID queryId);
    int countByQueryId(UUID queryId);
}
