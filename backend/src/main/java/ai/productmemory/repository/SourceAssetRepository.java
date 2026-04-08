package ai.productmemory.repository;

import ai.productmemory.domain.entity.SourceAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceAssetRepository extends JpaRepository<SourceAsset, UUID> {
    Page<SourceAsset> findByWorkspaceId(UUID workspaceId, Pageable pageable);
    Page<SourceAsset> findByWorkspaceIdAndProcessed(UUID workspaceId, Boolean processed, Pageable pageable);
    Optional<SourceAsset> findByWorkspaceIdAndContentHash(UUID workspaceId, String contentHash);
}
