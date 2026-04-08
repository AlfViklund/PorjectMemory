package ai.productmemory.repository;

import ai.productmemory.domain.entity.ImpactAnalysisOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImpactAnalysisOutputRepository extends JpaRepository<ImpactAnalysisOutput, UUID> {
    Optional<ImpactAnalysisOutput> findByChangeRequestId(UUID changeRequestId);
    Optional<ImpactAnalysisOutput> findByShortId(String shortId);
    boolean existsByChangeRequestIdAndStatus(UUID changeRequestId, String status);
}
