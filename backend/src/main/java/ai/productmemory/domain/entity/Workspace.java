package ai.productmemory.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Workspace — top-level tenant/project container.
 * Every other entity belongs to a Workspace.
 */
@Entity
@Table(name = "workspaces")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Optional GitHub/GitLab URL for primary repository */
    @Column(name = "primary_repo_url", length = 512)
    private String primaryRepoUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChangeRequest> changeRequests = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductMemoryItem> memoryItems = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SourceAsset> sourceAssets = new ArrayList<>();
}
