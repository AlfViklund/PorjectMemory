package ai.productmemory.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Requirement — извлечённое требование из SourceAsset или CR.
 * Производный артефакт Document Ingestion Pipeline.
 */
@Entity
@Table(name = "requirements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_section_id")
    private SourceSection sourceSection;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Direct grounding quote from source document */
    @Column(name = "grounding_quote", columnDefinition = "text")
    private String groundingQuote;

    /** Extracted confidence [0.0 - 1.0] */
    @Column(name = "confidence")
    private Double confidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
