package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GlobalFilterRepository extends JpaRepository<GlobalFilterEntity, UUID> {
    List<GlobalFilterEntity> findByDashboardId(UUID dashboardId);
}
