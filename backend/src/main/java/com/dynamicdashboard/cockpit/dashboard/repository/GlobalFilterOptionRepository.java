package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterOptionEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterOptionEntity.GlobalFilterOptionId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GlobalFilterOptionRepository extends JpaRepository<GlobalFilterOptionEntity, GlobalFilterOptionId> {
    List<GlobalFilterOptionEntity> findByIdFilterIdOrderByPositionIndexAsc(UUID filterId);
}
