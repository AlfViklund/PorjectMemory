package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Task — производный артефакт от ChangeRequest.
 * Не может перейти в EXECUTING без валидного TaskContextPack.
 *
 * Инвариант: task.status = EXECUTING только если linked TaskContextPack
 * существует и прошел валидацию (context_pack_validated_at != null).
 */
@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** External-facing short ID (e.g., task-a1b2c3d4) */
    @Column(name = "short_id", nullable = false, unique = true, length = 20)
    private String shortId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "rationale", columnDefinition = "text")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /** Timestamp when TaskContextPack was successfully validated */
    @Column(name = "context_pack_validated_at")
    private Instant contextPackValidatedAt;

    @Column(name = "ordinal")
    @Builder.Default
    private Integer ordinal = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Canonical execution artifact — stored separately, linked here */
    @OneToOne(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TaskContextPack contextPack;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExecutionRun> executionRuns = new ArrayList<>();
}
