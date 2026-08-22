package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DashboardRepository extends JpaRepository<DashboardEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = {"owner"})
    List<DashboardEntity> findAll();
    @Override
    @EntityGraph(attributePaths = {"owner"})
    Optional<DashboardEntity> findById(UUID id);
    @EntityGraph(attributePaths = {"owner"})
    List<DashboardEntity> findByOwnerId(UUID ownerId);

    long countByOwnerId(UUID ownerId);
}
