package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.SourceAssetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SourceAsset — любой загруженный документ или репозиторий.
 * Исходный материал для Document Ingestion или Repo Extraction.
 */
@Entity
@Table(name = "source_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private SourceAssetType assetType;

    /** S3/MinIO object key */
    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** Extracted raw text (may be large, stored in TEXT column) */
    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    /** Additional metadata as JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @OneToMany(mappedBy = "sourceAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SourceSection> sections = new ArrayList<>();
}
