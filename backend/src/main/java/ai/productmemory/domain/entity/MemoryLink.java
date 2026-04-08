package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.MemoryLinkType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MemoryLink — ребро графа Product Memory.
 * Реализует adjacency-list модель для реляционного графа без GraphDB.
 *
 * Пример: Screen A --[SCREEN_CALLS_API]--> API B
 */
@Entity
@Table(name = "memory_links",
    indexes = {
        @Index(name = "idx_ml_source", columnList = "source_item_id"),
        @Index(name = "idx_ml_target", columnList = "target_item_id"),
        @Index(name = "idx_ml_type", columnList = "link_type")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemoryLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_item_id", nullable = false)
    private ProductMemoryItem sourceItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_item_id", nullable = false)
    private ProductMemoryItem targetItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 60)
    private MemoryLinkType linkType;

    /** Confidence that this link is real [0.0 - 1.0] */
    @Column(name = "confidence")
    @Builder.Default
    private Double confidence = 1.0;

    @Column(name = "stale", nullable = false)
    @Builder.Default
    private Boolean stale = false;

    /** Optional description or grounding for this edge */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Extra properties as JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
