package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.ChangeRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ChangeRequest — центральный операционный объект.
 * Все changes начинаются здесь. Task является ПРОИЗВОДНЫМ от CR.
 *
 * Инварианты:
 * - нельзя перейти в IN_PLANNING без валидного ImpactAnalysisOutput
 * - нельзо перейти в MERGED без утверждённого MemoryMergePlan
 */
@Entity
@Table(name = "change_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** External-facing short ID (e.g., cr-a1b2c3d4) */
    @Column(name = "short_id", nullable = false, unique = true, length = 20)
    private String shortId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "intent", columnDefinition = "text", nullable = false)
    private String intent;

    @Column(name = "rationale", columnDefinition = "text")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ChangeRequestStatus status = ChangeRequestStatus.PROPOSED;

    /** Priority 1 (highest) - 5 (lowest) */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 3;

    /** JSONB: additional metadata, tags, labels */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    /** Link to the ImpactAnalysis — REQUIRED before planning */
    @OneToOne(mappedBy = "changeRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ImpactAnalysisOutput impactAnalysis;

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Requirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Finding> findings = new ArrayList<>();
}
