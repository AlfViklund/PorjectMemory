package ai.productmemory.repository;

import ai.productmemory.domain.entity.ChangeRequest;
import ai.productmemory.domain.enums.ChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {
    Optional<ChangeRequest> findByShortId(String shortId);
    Page<ChangeRequest> findByWorkspaceId(UUID workspaceId, Pageable pageable);
    Page<ChangeRequest> findByWorkspaceIdAndStatus(UUID workspaceId, ChangeRequestStatus status, Pageable pageable);
    List<ChangeRequest> findByWorkspaceIdAndStatusIn(UUID workspaceId, List<ChangeRequestStatus> statuses);

    @Query("SELECT cr FROM ChangeRequest cr LEFT JOIN FETCH cr.impactAnalysis WHERE cr.id = :id")
    Optional<ChangeRequest> findByIdWithImpactAnalysis(UUID id);
}
