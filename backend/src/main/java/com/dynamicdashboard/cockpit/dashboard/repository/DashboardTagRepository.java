package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardTagEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardTagEntity.DashboardTagId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DashboardTagRepository extends JpaRepository<DashboardTagEntity, DashboardTagId> {
    List<DashboardTagEntity> findByIdDashboardId(UUID dashboardId);
}
