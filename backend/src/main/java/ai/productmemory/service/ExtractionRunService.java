package ai.productmemory.service;

import ai.productmemory.domain.entity.*;
import ai.productmemory.domain.enums.*;
import ai.productmemory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * ExtractionRunService — Phase 3.
 *
 * An ExtractionRun represents one attempt to extract structured knowledge
 * (Requirements, Capabilities, Memory Items) from a RepoSnapshot.
 *
 * INVARIANT: Every extracted artifact MUST be linked to an ExtractionRun,
 * which MUST reference a RepoSnapshot. This ensures all memory is grounded
 * in a specific, reproducible repository state.
 *
 * No RepoSnapshot → no ExtractionRun → no Memory Items can be created.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionRunService {

    private final ExtractionRunRepository extractionRunRepository;
    private final RepoSnapshotRepository repoSnapshotRepository;
    private final ProductMemoryItemRepository memoryItemRepository;
    private final WorkspaceRepository workspaceRepository;

    /**
     * Phase 3: Start and execute an extraction run against a snapshot.
     * Creates PENDING run → sets RUNNING → extracts → completes or fails.
     */
    @Transactional
    public ExtractionRun runExtraction(UUID snapshotId, UUID workspaceId) {
        RepoSnapshot snapshot = repoSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("RepoSnapshot not found: " + snapshotId));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // Create ExtractionRun record
        ExtractionRun run = ExtractionRun.builder()
                .repoSnapshot(snapshot)
                .workspace(workspace)
                .status(ExtractionRunStatus.PENDING)
                .startedAt(Instant.now())
                .extractorVersion("mvp-heuristic-1.0")
                .extractorCategory("code_heuristic")
                .build();
        run = extractionRunRepository.save(run);

        log.info("Started ExtractionRun {} for snapshot {} (commit: {})",
                run.getId(), snapshotId,
                snapshot.getCommitSha() != null
                        ? snapshot.getCommitSha().substring(0, Math.min(8, snapshot.getCommitSha().length()))
                        : "unknown");

        try {
            run.setStatus(ExtractionRunStatus.RUNNING);
            run.setStartedAt(Instant.now());
            run = extractionRunRepository.save(run);

            // Run extraction logic
            ExtractionResult result = extractFromSnapshot(snapshot, workspace, run);

            run.setStatus(ExtractionRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            run.setItemsExtracted(result.totalItems());
            run.setErrorMessage(null);

            log.info("ExtractionRun {} completed: {} items extracted", run.getId(), result.totalItems());

        } catch (Exception e) {
            run.setStatus(ExtractionRunStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            log.error("ExtractionRun {} failed: {}", run.getId(), e.getMessage(), e);
        }

        return extractionRunRepository.save(run);
    }

    /**
     * Core extraction logic: walks file manifest and creates ProductMemoryItems.
     * MVP: heuristic pattern-based.
     * V2+: LLM-based with grounded quotes.
     */
    private ExtractionResult extractFromSnapshot(RepoSnapshot snapshot, Workspace workspace,
                                                  ExtractionRun run) {
        int itemCount = 0;
        Map<String, String> manifest = snapshot.getFileManifest();

        if (manifest == null || manifest.isEmpty()) {
            log.warn("Snapshot {} has empty file manifest — nothing to extract", snapshot.getId());
            return new ExtractionResult(0);
        }

        for (Map.Entry<String, String> entry : manifest.entrySet()) {
            String filePath = entry.getKey();
            ProductMemoryItemType itemType = classifyFile(filePath);
            if (itemType == null) continue;

            String itemName = deriveItemName(filePath);

            // Idempotency: skip if same item (name + type + snapshot) already exists
            boolean exists = memoryItemRepository.existsByWorkspaceIdAndItemTypeAndNameAndGroundingRef(
                    workspace.getId(), itemType, itemName, snapshot.getCommitSha() + ":" + filePath);
            if (exists) continue;

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("sourceFile", filePath);
            properties.put("fileHash", entry.getValue());
            properties.put("snapshotId", snapshot.getId().toString());
            properties.put("extractorVersion", run.getExtractorVersion());

            ProductMemoryItem item = ProductMemoryItem.builder()
                    .workspace(workspace)
                    .itemType(itemType)
                    .name(itemName)
                    .description("Auto-extracted from " + filePath)
                    .status(MemoryItemStatus.PROPOSED)
                    .confidence(0.6) // heuristic confidence
                    .sourceType("repo_extraction")
                    .groundingRef(snapshot.getCommitSha() != null
                            ? snapshot.getCommitSha() + ":" + filePath
                            : snapshot.getTreeHash().substring(0, 12) + ":" + filePath)
                    .properties(properties)
                    .build();

            memoryItemRepository.save(item);
            itemCount++;
        }

        return new ExtractionResult(itemCount);
    }

    /**
     * Heuristic file classifier → ProductMemoryItemType.
     */
    private ProductMemoryItemType classifyFile(String filePath) {
        String lower = filePath.toLowerCase();

        // API endpoints
        if (lower.contains("controller") || lower.contains("router") || lower.contains("handler"))
            return ProductMemoryItemType.API_ENDPOINT;

        // Screens / UI
        if (lower.contains("page") || lower.contains("screen") || lower.contains("view")
                || lower.endsWith(".tsx") || lower.endsWith(".jsx"))
            return ProductMemoryItemType.SCREEN;

        // Data entities
        if (lower.contains("entity") || lower.contains("model") || lower.contains("domain")
                || lower.contains("schema"))
            return ProductMemoryItemType.DATA_ENTITY;

        // Services → Capabilities
        if (lower.contains("service") || lower.contains("usecase") || lower.contains("interactor"))
            return ProductMemoryItemType.CAPABILITY;

        // Config / SQL → skip (not meaningful memory items)
        if (lower.endsWith(".sql") || lower.contains("config") || lower.contains("migration"))
            return null;

        // Default: if Java/Kotlin/Py - treat as capability
        if (lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".py"))
            return ProductMemoryItemType.CAPABILITY;

        return null;
    }

    /**
     * Derives a human-readable name from a file path.
     * e.g., "src/main/java/.../ProductMemoryController.java" → "ProductMemoryController"
     */
    private String deriveItemName(String filePath) {
        String[] parts = filePath.replace('\\', '/').split("/");
        String fileName = parts[parts.length - 1];
        // Remove extension
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
    }

    private record ExtractionResult(int totalItems) {}

    /**
     * Get all extraction runs for a workspace, ordered by newest first.
     */
    @Transactional(readOnly = true)
    public List<ExtractionRun> listByWorkspace(UUID workspaceId) {
        return extractionRunRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    /**
     * Get extraction history for a specific snapshot.
     */
    @Transactional(readOnly = true)
    public List<ExtractionRun> listBySnapshot(UUID snapshotId) {
        return extractionRunRepository.findByRepoSnapshotId(snapshotId);
    }
}
