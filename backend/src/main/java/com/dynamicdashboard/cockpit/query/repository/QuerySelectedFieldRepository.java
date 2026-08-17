package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QuerySelectedFieldEntity;
import com.dynamicdashboard.cockpit.query.domain.QuerySelectedFieldEntity.QuerySelectedFieldId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuerySelectedFieldRepository extends JpaRepository<QuerySelectedFieldEntity, QuerySelectedFieldId> {
    List<QuerySelectedFieldEntity> findByIdQueryIdOrderByPositionIndexAsc(UUID queryId);
}
