package com.dynamicdashboard.cockpit.dashboard.repository;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterValueMapEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterValueMapEntity.GlobalFilterValueMapId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GlobalFilterValueMapRepository
        extends JpaRepository<GlobalFilterValueMapEntity, GlobalFilterValueMapId> {
    List<GlobalFilterValueMapEntity> findByIdFilterId(UUID filterId);
}
