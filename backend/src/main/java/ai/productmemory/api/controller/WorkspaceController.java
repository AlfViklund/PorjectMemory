package ai.productmemory.api.controller;

import ai.productmemory.api.dto.ApiResponse;
import ai.productmemory.domain.entity.Workspace;
import ai.productmemory.repository.WorkspaceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;

    public record CreateWorkspaceRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 100) String slug,
            String description,
            String primaryRepoUrl
    ) {}

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody CreateWorkspaceRequest req) {
        Workspace ws = Workspace.builder()
                .name(req.name())
                .slug(req.slug())
                .description(req.description())
                .primaryRepoUrl(req.primaryRepoUrl())
                .build();
        ws = workspaceRepository.save(ws);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toMap(ws)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        List<Workspace> workspaces = workspaceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(workspaces.stream().map(this::toMap).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable UUID id) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok(toMap(ws)));
    }

    private Map<String, Object> toMap(Workspace ws) {
        return Map.of(
                "id", ws.getId(),
                "name", ws.getName(),
                "slug", ws.getSlug(),
                "description", ws.getDescription() != null ? ws.getDescription() : "",
                "primaryRepoUrl", ws.getPrimaryRepoUrl() != null ? ws.getPrimaryRepoUrl() : "",
                "createdAt", ws.getCreatedAt()
        );
    }
}
