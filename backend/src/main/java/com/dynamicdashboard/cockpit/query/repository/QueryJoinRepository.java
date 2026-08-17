package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QueryJoinEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QueryJoinRepository extends JpaRepository<QueryJoinEntity, UUID> {
    List<QueryJoinEntity> findByQueryId(UUID queryId);
}
