package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.MergePlanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MemoryMergePlan — явный персистентный план слияния памяти.
 *
 * ПРАВИЛА (финально зафиксированы в addendum):
 * 1. Memory НЕ МОЖЕТ обновляться неявно.
 * 2. Формируется после review как persisted artifact с ID.
 * 3. Reviewer утверждает план целиком или частично (partial approval).
 * 4. Утверждённый subset применяется АТОМАРНО.
 * 5. Сам план сохраняется как audit artifact навсегда.
 * 6. Suggested follow-ups могут генерироваться, НО новые CR не создаются автоматически.
 *
 * Соответствует JSON-схеме MemoryMergePlan из ProjectMemoryOS.md.
 */
@Entity
@Table(name = "memory_merge_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemoryMergePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** External short ID: e.g., mm-a1b2c3d4 */
    @Column(name = "short_id", nullable = false, unique = true, length = 20)
    private String shortId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "schema_version", nullable = false, length = 20)
    @Builder.Default
    private String schemaVersion = "1.0";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MergePlanStatus status = MergePlanStatus.PENDING_REVIEW;

    /**
     * JSONB: core merge plan payload:
     * changesToApply: { create[], update[], markAsStale[] }
     * potentialConflicts[]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    /**
     * JSONB: which items in the plan were approved (for partial approval).
     * Structure: { "approvedItems": ["id1", "id2"], "rejectedItems": ["id3"] }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "approval_decisions", columnDefinition = "jsonb")
    private Map<String, Object> approvalDecisions;

    /**
     * JSONB: suggested follow-ups (type, title, rationale).
     * These are NEVER auto-created as CRs without human confirmation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_follow_ups", columnDefinition = "jsonb")
    private Map<String, Object> suggestedFollowUps;

    @Column(name = "reviewer_id", length = 255)
    private String reviewerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    /** Error message if application failed */
    @Column(name = "application_error", columnDefinition = "text")
    private String applicationError;
}
