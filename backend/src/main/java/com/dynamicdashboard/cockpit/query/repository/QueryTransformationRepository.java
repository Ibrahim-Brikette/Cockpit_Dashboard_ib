package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QueryTransformationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QueryTransformationRepository extends JpaRepository<QueryTransformationEntity, UUID> {
    List<QueryTransformationEntity> findByQueryId(UUID queryId);
}
