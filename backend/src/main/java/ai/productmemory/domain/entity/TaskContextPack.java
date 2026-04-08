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
 * TaskContextPack — канонический артефакт выполнения.
 *
 * ПРАВИЛА:
 * 1. Хранится как persisted artifact (не in-memory construct).
 * 2. Имеет schema_version для валидации.
 * 3. Runtime adapter ОБЯЗАН принимать именно этот артефакт.
 * 4. Нельзя собирать контекст самостоятельно в runtime.
 * 5. Execution блокируется при unresolved conflicts.
 *
 * Соответствует JSON-схеме TaskContextPack из ProjectMemoryOS.md.
 */
@Entity
@Table(name = "task_context_packs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskContextPack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** External short ID: e.g., tcp-a1b2c3d4 */
    @Column(name = "short_id", nullable = false, unique = true, length = 20)
    private String shortId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "schema_version", nullable = false, length = 20)
    @Builder.Default
    private String schemaVersion = "1.0";

    /** VALID | INVALID | CONFLICT */
    @Column(name = "validation_status", nullable = false, length = 20)
    @Builder.Default
    private String validationStatus = "PENDING";

    /** If conflicts exist and are unresolved, execution is BLOCKED */
    @Column(name = "has_unresolved_conflicts", nullable = false)
    @Builder.Default
    private Boolean hasUnresolvedConflicts = false;

    /**
     * JSONB strict-core payload matching TaskContextPack JSON Schema:
     * taskIdentity, linkedChangeRequest, summary, rationale,
     * acceptanceCriteria, impactedProductAreas, scope, outOfScope,
     * expectedDeliverables, runtimeContext, postExecutionMemoryUpdatePlan
     *
     * additionalProperties in core are forbidden (enforced by service layer).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    /**
     * JSONB extension section — allowed flexible extensions here only.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extensions", columnDefinition = "jsonb")
    private Map<String, Object> extensions;

    /** Source precedence for conflict resolution */
    @Column(name = "source_precedence", length = 50)
    private String sourcePrecedence;

    @Column(name = "conflict_details", columnDefinition = "text")
    private String conflictDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "validated_at")
    private Instant validatedAt;
}
