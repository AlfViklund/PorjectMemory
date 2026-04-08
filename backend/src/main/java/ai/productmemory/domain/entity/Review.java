package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Review — Human Review Gate.
 * После Review формируется MemoryMergePlan.
 * Partial approval допустим: PARTIALLY_APPROVED.
 */
@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** External-facing short ID (e.g., rv-a1b2c3d4) */
    @Column(name = "short_id", nullable = false, unique = true, length = 20)
    private String shortId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "reviewer_id", length = 255)
    private String reviewerId;

    @Column(name = "reviewer_comment", columnDefinition = "text")
    private String reviewerComment;

    /** JSONB: which specific items were approved/rejected in partial approval */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "approval_details", columnDefinition = "jsonb")
    private Map<String, Object> approvalDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Resulting MemoryMergePlan — created after review decision */
    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MemoryMergePlan memoryMergePlan;
}
