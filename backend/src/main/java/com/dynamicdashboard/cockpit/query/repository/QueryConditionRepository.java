package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QueryConditionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QueryConditionRepository extends JpaRepository<QueryConditionEntity, UUID> {
    List<QueryConditionEntity> findByQueryId(UUID queryId);
}
