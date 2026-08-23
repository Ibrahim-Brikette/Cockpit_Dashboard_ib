package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.QuerySortEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuerySortRepository extends JpaRepository<QuerySortEntity, UUID> {
}
