package ai.productmemory.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * SourceSection — элемент извлечённой секции документа.
 * Создаётся в процессе Sectioning шага Document Ingestion Pipeline.
 */
@Entity
@Table(name = "source_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_asset_id", nullable = false)
    private SourceAsset sourceAsset;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    /** Ordinal index of this section within the document */
    @Column(name = "section_index", nullable = false)
    private Integer sectionIndex;

    /** Heading level (e.g., 1 = H1, 2 = H2) */
    @Column(name = "heading_level")
    private Integer headingLevel;

    /** Source line/byte offsets for traceability */
    @Column(name = "start_offset")
    private Long startOffset;

    @Column(name = "end_offset")
    private Long endOffset;
}
