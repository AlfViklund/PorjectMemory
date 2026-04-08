package ai.productmemory.repository;

import ai.productmemory.domain.entity.ExtractionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {

    List<ExtractionRun> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<ExtractionRun> findByRepoSnapshotId(UUID snapshotId);
}
