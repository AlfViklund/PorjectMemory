package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.FindingSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Finding — расхождение или наблюдение, обнаруженное в ходе Reconciliation.
 * Может генерировать предложения по follow-up CR, но НЕ создает CR автоматически.
 */
@Entity
@Table(name = "findings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_run_id")
    private ExecutionRun executionRun;

    /** Impacted memory item, if applicable */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_item_id")
    private ProductMemoryItem memoryItem;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "text", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private FindingSeverity severity = FindingSeverity.MEDIUM;

    /** Type: stale_memory, missing_evidence, acceptance_failure, conflict, etc. */
    @Column(name = "finding_type", length = 50)
    private String findingType;

    /** Suggested follow-up action (plain text) — does NOT auto-create a CR */
    @Column(name = "suggested_follow_up", columnDefinition = "text")
    private String suggestedFollowUp;

    /** If true, a human has acknowledged this finding */
    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    /** JSONB: structured details */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
