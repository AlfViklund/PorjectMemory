package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.MemoryItemStatus;
import ai.productmemory.domain.enums.ProductMemoryItemType;
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
 * ProductMemoryItem — узел графа Product Memory.
 * Это могут быть: Capability, Screen, API, DataEntity, Decision и т.д.
 *
 * Граф реализован реляционно (adjacency-list через MemoryLink),
 * без выделенной графовой БД.
 */
@Entity
@Table(name = "product_memory_items",
    indexes = {
        @Index(name = "idx_pmi_workspace_type", columnList = "workspace_id, item_type"),
        @Index(name = "idx_pmi_status", columnList = "status")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMemoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private ProductMemoryItemType itemType;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MemoryItemStatus status = MemoryItemStatus.PROPOSED;

    /** Confidence score [0.0 - 1.0] that this item accurately reflects reality */
    @Column(name = "confidence")
    @Builder.Default
    private Double confidence = 0.5;

    /** Where did this item originate: document_ingestion, repo_extraction, manual, cr_planning */
    @Column(name = "source_type", length = 50)
    private String sourceType;

    /** Reference back to ExtractionRun if from repo extraction */
    @Column(name = "extraction_run_id")
    private UUID extractionRunId;

    /** Reference back to SourceSection if from document ingestion */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_section_id")
    private SourceSection sourceSection;

    /** Grounding reference in code: file path + line */
    @Column(name = "grounding_ref", length = 1024)
    private String groundingRef;

    /** Flexible JSONB payload — type-specific properties */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @Column(name = "stale_reason", columnDefinition = "text")
    private String staleReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Outgoing edges from this node */
    @OneToMany(mappedBy = "sourceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoryLink> outgoingLinks = new ArrayList<>();

    /** Incoming edges to this node */
    @OneToMany(mappedBy = "targetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoryLink> incomingLinks = new ArrayList<>();
}
