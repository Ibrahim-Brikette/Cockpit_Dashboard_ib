package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetDatagridConfigEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface WidgetDatagridConfigRepository extends JpaRepository<WidgetDatagridConfigEntity, UUID> {
    @Modifying
    @Query("DELETE FROM WidgetDatagridConfigEntity c WHERE c.widgetId IN (SELECT w.id FROM WidgetEntity w WHERE w.dashboard.id = :dashboardId)")
    void deleteByDashboardId(@Param("dashboardId") UUID dashboardId);
}
