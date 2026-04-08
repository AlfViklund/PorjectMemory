package ai.productmemory.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RepoSnapshot — неизменяемая привязка к конкретному состоянию репозитория.
 * Все артефакты Existing-project mode ОБЯЗАНЫ ссылаться на этот снэпшот.
 *
 * treeHash — детерминированный SHA-256 хэш всего дерева файлов.
 * Без repo_snapshot_id результаты extraction не воспроизводимы.
 */
@Entity
@Table(name = "repo_snapshots",
    indexes = {
        @Index(name = "idx_repo_snapshots_workspace", columnList = "workspace_id"),
        @Index(name = "idx_repo_snapshots_tree_hash", columnList = "tree_hash")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "repo_url", nullable = false, length = 512)
    private String repoUrl;

    @Column(name = "branch", length = 255)
    private String branch;

    /** Full 40-char commit SHA */
    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    /** Tree SHA (alternative to commit_sha for specific paths) */
    @Column(name = "tree_sha", length = 40)
    private String treeSha;

    /**
     * Deterministic SHA-256 of sorted "path:fileHash\n" lines.
     * Same tree hash = same file contents = reproducible extraction.
     */
    @Column(name = "tree_hash", length = 64)
    private String treeHash;

    /** Whether the working tree was dirty at snapshot time */
    @Column(name = "dirty_state", nullable = false)
    @Builder.Default
    private Boolean dirtyState = false;

    /** Total number of tracked source files in snapshot */
    @Column(name = "total_files")
    @Builder.Default
    private Integer totalFiles = 0;

    /** Hash of all extracted batch content for reproducibility */
    @Column(name = "batch_content_hash", length = 64)
    private String batchContentHash;

    /** JSONB: file path → file SHA-256 manifest */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_manifest", columnDefinition = "jsonb")
    private Map<String, String> fileManifest;

    /** JSONB: extra snapshot metadata (tags, etc.) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /** When this snapshot was taken */
    @Column(name = "snapshot_taken_at")
    private Instant snapshotTakenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "repoSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExtractionRun> extractionRuns = new ArrayList<>();
}
