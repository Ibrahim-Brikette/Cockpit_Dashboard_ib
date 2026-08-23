package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetDatagridVisibleColumnEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetDatagridVisibleColumnEntity.WidgetDatagridVisibleColumnId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface WidgetDatagridVisibleColumnRepository
        extends JpaRepository<WidgetDatagridVisibleColumnEntity, WidgetDatagridVisibleColumnId> {
    List<WidgetDatagridVisibleColumnEntity> findByIdWidgetIdOrderByPositionIndexAsc(UUID widgetId);
    @Modifying
    @Query("DELETE FROM WidgetDatagridVisibleColumnEntity v WHERE v.id.widgetId IN (SELECT w.id FROM WidgetEntity w WHERE w.dashboard.id = :dashboardId)")
    void deleteByDashboardId(@Param("dashboardId") UUID dashboardId);
}
