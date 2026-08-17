package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QueryGroupByFieldEntity;
import com.dynamicdashboard.cockpit.query.domain.QueryGroupByFieldEntity.QueryGroupByFieldId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QueryGroupByFieldRepository extends JpaRepository<QueryGroupByFieldEntity, QueryGroupByFieldId> {
    List<QueryGroupByFieldEntity> findByIdQueryIdOrderByPositionIndexAsc(UUID queryId);
}
