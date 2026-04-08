package ai.productmemory.domain.entity;

import ai.productmemory.domain.enums.ExecutionRunStatus;
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
 * ExecutionRun — один запуск Task в контейнерной среде.
 * Всегда ссылается на TaskContextPack, который был активен при запуске.
 */
@Entity
@Table(name = "execution_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExecutionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** Snapshot of the TaskContextPack used for this run */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_context_pack_id")
    private TaskContextPack taskContextPack;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ExecutionRunStatus status = ExecutionRunStatus.QUEUED;

    /** Docker image used for this run */
    @Column(name = "docker_image", length = 512)
    private String dockerImage;

    /** Container ID assigned by Docker daemon */
    @Column(name = "container_id", length = 128)
    private String containerId;

    /** Stdout log */
    @Column(name = "stdout_log", columnDefinition = "text")
    private String stdoutLog;

    /** Stderr log */
    @Column(name = "stderr_log", columnDefinition = "text")
    private String stderrLog;

    /** Exit code from the container */
    @Column(name = "exit_code")
    private Integer exitCode;

    /** JSONB: runtime metadata — env vars used, mounts, etc. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "runtime_metadata", columnDefinition = "jsonb")
    private Map<String, Object> runtimeMetadata;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "executionRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Evidence> evidences = new ArrayList<>();
}
