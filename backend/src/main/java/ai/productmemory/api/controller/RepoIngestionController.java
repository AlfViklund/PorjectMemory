package ai.productmemory.api.controller;

import ai.productmemory.api.dto.ApiResponse;
import ai.productmemory.domain.entity.ExtractionRun;
import ai.productmemory.domain.entity.RepoSnapshot;
import ai.productmemory.service.ExtractionRunService;
import ai.productmemory.service.RepoIngestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RepoIngestionController — Phase 3.
 * Snapshot repos, detect drift, list extraction runs.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/repo")
@RequiredArgsConstructor
public class RepoIngestionController {

    private final RepoIngestionService repoIngestionService;
    private final ExtractionRunService extractionRunService;

    public record SnapshotRequest(
            @NotBlank String localPath,
            String branch
    ) {}

    /**
     * POST /repo/snapshots — create a snapshot of a local repo path.
     * Idempotent: same tree hash → returns existing snapshot.
     */
    @PostMapping("/snapshots")
    public ResponseEntity<ApiResponse<Map<String, Object>>> snapshot(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SnapshotRequest req) {
        RepoSnapshot snapshot = repoIngestionService.snapshotLocalRepo(
                workspaceId, req.localPath(), req.branch());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toSnapshotMap(snapshot)));
    }

    /**
     * GET /repo/snapshots — list all snapshots for workspace.
     */
    @GetMapping("/snapshots")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listSnapshots(
            @PathVariable UUID workspaceId) {
        // Use RepoIngestionService to detect drift against latest
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }

    /**
     * GET /repo/drift — detect drift between current repo state and latest snapshot.
     */
    @GetMapping("/drift")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detectDrift(
            @PathVariable UUID workspaceId,
            @RequestParam String localPath) {
        RepoIngestionService.SnapshotDiff diff = repoIngestionService.detectDrift(workspaceId, localPath);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "hasDrift", diff.hasDrift(),
                "summary", diff.summary(),
                "currentTreeHash", diff.currentTreeHash() != null ? diff.currentTreeHash().substring(0, 12) + "..." : "none",
                "previousTreeHash", diff.previousTreeHash() != null ? diff.previousTreeHash().substring(0, 12) + "..." : "none",
                "currentFileCount", diff.currentFileCount(),
                "previousFileCount", diff.previousFileCount()
        )));
    }

    /**
     * GET /repo/extraction-runs — list all extraction runs for workspace.
     */
    @GetMapping("/extraction-runs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listExtractionRuns(
            @PathVariable UUID workspaceId) {
        List<ExtractionRun> runs = extractionRunService.listByWorkspace(workspaceId);
        List<Map<String, Object>> result = runs.stream().map(this::toRunMap).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private Map<String, Object> toSnapshotMap(RepoSnapshot s) {
        return Map.of(
                "id", s.getId(),
                "workspaceId", s.getWorkspace().getId(),
                "repoUrl", s.getRepoUrl(),
                "branch", s.getBranch() != null ? s.getBranch() : "unknown",
                "commitSha", s.getCommitSha() != null ? s.getCommitSha() : "unknown",
                "treeHash", s.getTreeHash() != null ? s.getTreeHash().substring(0, 12) + "..." : "none",
                "totalFiles", s.getTotalFiles(),
                "snapshotTakenAt", s.getSnapshotTakenAt(),
                "createdAt", s.getCreatedAt()
        );
    }

    private Map<String, Object> toRunMap(ExtractionRun r) {
        return Map.of(
                "id", r.getId(),
                "snapshotId", r.getRepoSnapshot().getId(),
                "status", r.getStatus().name(),
                "extractorVersion", r.getExtractorVersion(),
                "itemsExtracted", r.getItemsExtracted() != null ? r.getItemsExtracted() : 0,
                "startedAt", r.getStartedAt() != null ? r.getStartedAt() : "",
                "completedAt", r.getCompletedAt() != null ? r.getCompletedAt() : "",
                "errorMessage", r.getErrorMessage() != null ? r.getErrorMessage() : ""
        );
    }
}
