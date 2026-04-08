package ai.productmemory.repository;

import ai.productmemory.domain.entity.RepoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepoSnapshotRepository extends JpaRepository<RepoSnapshot, UUID> {

    List<RepoSnapshot> findByWorkspaceId(UUID workspaceId);

    Optional<RepoSnapshot> findByWorkspaceIdAndCommitSha(UUID workspaceId, String commitSha);

    Optional<RepoSnapshot> findByWorkspaceIdAndTreeHash(UUID workspaceId, String treeHash);

    @Query("""
            SELECT s FROM RepoSnapshot s
            WHERE s.workspace.id = :workspaceId
            ORDER BY s.snapshotTakenAt DESC
            LIMIT 1
            """)
    Optional<RepoSnapshot> findLatestByWorkspaceId(UUID workspaceId);

    List<RepoSnapshot> findByWorkspaceIdOrderBySnapshotTakenAtDesc(UUID workspaceId);
}
