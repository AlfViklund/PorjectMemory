package ai.productmemory.repository;

import ai.productmemory.domain.entity.RuntimeCapabilityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuntimeCapabilityReportRepository extends JpaRepository<RuntimeCapabilityReport, UUID> {
    Optional<RuntimeCapabilityReport> findTopByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
