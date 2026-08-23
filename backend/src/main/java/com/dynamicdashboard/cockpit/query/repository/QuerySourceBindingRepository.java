package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QuerySourceBindingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuerySourceBindingRepository extends JpaRepository<QuerySourceBindingEntity, UUID> {
    List<QuerySourceBindingEntity> findByQueryIdOrderByPositionIndexAsc(UUID queryId);
}
