package ai.productmemory.repository;

import ai.productmemory.domain.entity.Finding;
import ai.productmemory.domain.enums.FindingSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<Finding, UUID> {
    List<Finding> findByChangeRequestId(UUID changeRequestId);
    List<Finding> findByMemoryItemId(UUID memoryItemId);
    List<Finding> findBySeverityAndAcknowledged(FindingSeverity severity, Boolean acknowledged);
    List<Finding> findByChangeRequestIdAndAcknowledged(UUID changeRequestId, Boolean acknowledged);
}
