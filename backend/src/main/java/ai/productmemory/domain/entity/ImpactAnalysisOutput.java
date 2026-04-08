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
 * ImpactAnalysisOutput — обязательный персистентный артефакт перед planning.
 *
 * GATE: Если этот артефакт отсутствует или имеет status != COMPLETED,
 * ChangeRequest НЕ МОЖЕТ перейти в IN_PLANNING.
 *
 * Соответствует JSON-схеме из ProjectMemoryOS.md § ImpactAnalysisOutput.
 * schema_version позволяет валидировать схему при EvolveIt.
 */
@Entity
@Table(name = "impact_analysis_outputs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImpactAnalysisOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** External short ID: e.g., ia-a1b2c3d4 */
    @Column(name = "short_id", nullable = false, unique = true, length = 20)
    private String shortId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @Column(name = "schema_version", nullable = false, length = 20)
    @Builder.Default
    private String schemaVersion = "1.0";

    /** PENDING | RUNNING | COMPLETED | FAILED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * JSONB: core payload matching ImpactAnalysisOutput JSON Schema.
     * Contains: impactedCapabilities, impactedScreens, impactedAPIs,
     * suspectedDependencyConflicts, recommendedExecutionScope,
     * recommendedReviewScope, findingsGenerated
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
