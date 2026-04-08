package ai.productmemory.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Evidence — доказательство из ExecutionRun.
 * Подтверждает или опровергает выполнение критериев приёмки.
 * Недействительное если собрано без TaskContextPack.
 */
@Entity
@Table(name = "evidences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_run_id", nullable = false)
    private ExecutionRun executionRun;

    /** Type: file_output, test_result, log_excerpt, api_response, etc. */
    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Reference to acceptance criterion from TaskContextPack */
    @Column(name = "acceptance_criterion_id", length = 100)
    private String acceptanceCriterionId;

    /** Outcome: PASS / FAIL / INCONCLUSIVE */
    @Column(name = "outcome", length = 20)
    private String outcome;

    /** S3/MinIO key if evidence is a file artifact */
    @Column(name = "storage_key", length = 1024)
    private String storageKey;

    /** Inline content (small payloads) */
    @Column(name = "content", columnDefinition = "text")
    private String content;

    /** JSONB: structured evidence payload */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /** True if evidence was collected without a valid TaskContextPack — INVALID */
    @Column(name = "invalid", nullable = false)
    @Builder.Default
    private Boolean invalid = false;

    @Column(name = "invalid_reason", length = 512)
    private String invalidReason;

    @CreationTimestamp
    @Column(name = "collected_at", nullable = false, updatable = false)
    private Instant collectedAt;
}
