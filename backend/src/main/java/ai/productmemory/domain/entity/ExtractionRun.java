package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.ExtractionRunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ExtractionRun — один запуск процесса извлечения данных из репозитория.
 * Привязан к RepoSnapshot И Workspace.
 * INVARIANT: каждый ProductMemoryItem от repo extraction ссылается на этот run.
 */
@Entity
@Table(name = "extraction_runs",
    indexes = {
        @Index(name = "idx_extraction_runs_snapshot", columnList = "repo_snapshot_id"),
        @Index(name = "idx_extraction_runs_workspace", columnList = "workspace_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExtractionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repo_snapshot_id", nullable = false)
    private RepoSnapshot repoSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /** Semantic version of the extractor used */
    @Column(name = "extractor_version", nullable = false, length = 50)
    private String extractorVersion;

    /** Which extractor category: project_structure, api, frontend, data, delivery */
    @Column(name = "extractor_category", length = 50)
    private String extractorCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ExtractionRunStatus status = ExtractionRunStatus.PENDING;

    /** Number of items extracted */
    @Column(name = "items_extracted")
    @Builder.Default
    private Integer itemsExtracted = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** JSONB: extractor-specific configuration */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    /** JSONB: summary of what was extracted */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_summary", columnDefinition = "jsonb")
    private Map<String, Object> resultSummary;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
