package ai.productmemory.service;

import ai.productmemory.domain.entity.RepoSnapshot;
import ai.productmemory.domain.entity.Workspace;
import ai.productmemory.repository.RepoSnapshotRepository;
import ai.productmemory.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;


/**
 * RepoIngestionService — Phase 3.
 *
 * Snapshot Identity Pipeline:
 * 1. Create RepoSnapshot from a local path (or future: git clone URL)
 * 2. Walk all source files, compute per-file SHA-256
 * 3. Combine into a deterministic tree hash (reproducibility)
 * 4. Persist snapshot → all subsequent ExtractionRuns reference this snapshot
 *
 * This ensures: every artifact (Requirement, Capability, MemoryItem)
 * can be traced back to the exact repo state that produced it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepoIngestionService {

    private final RepoSnapshotRepository repoSnapshotRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ExtractionRunService extractionRunService;

    private static final List<String> TRACKED_EXTENSIONS = List.of(
            ".java", ".kt", ".ts", ".tsx", ".js", ".jsx", ".py",
            ".go", ".rs", ".yaml", ".yml", ".json", ".md", ".sql"
    );

    /**
     * Phase 3 Step 1-3: Snapshot a local repository path.
     * Idempotent: if same tree hash already exists, returns existing snapshot.
     */
    @Transactional
    public RepoSnapshot snapshotLocalRepo(UUID workspaceId, String localPath, String branch) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        Path repoRoot = Path.of(localPath);
        if (!Files.isDirectory(repoRoot)) {
            throw new IllegalArgumentException("Not a directory: " + localPath);
        }

        log.info("Snapshotting repo at {} (branch: {})", localPath, branch);

        // Walk files and build tree manifest
        Map<String, String> fileHashes = walkAndHash(repoRoot);
        String treeHash = computeTreeHash(fileHashes);
        long totalFiles = fileHashes.size();

        log.info("Repo snapshot: {} files, tree hash: {}", totalFiles, treeHash.substring(0, 12) + "...");

        // Idempotency: same tree hash = same snapshot
        Optional<RepoSnapshot> existing = repoSnapshotRepository
                .findByWorkspaceIdAndTreeHash(workspaceId, treeHash);
        if (existing.isPresent()) {
            log.info("Snapshot with tree hash {} already exists: {}", treeHash.substring(0, 12), existing.get().getId());
            return existing.get();
        }

        // Git metadata (best-effort, no hard dependency on git CLI)
        String headCommitSha = resolveHeadCommit(repoRoot);
        String resolvedBranch = branch != null ? branch : resolveBranch(repoRoot);

        RepoSnapshot snapshot = RepoSnapshot.builder()
                .workspace(workspace)
                .repoUrl(localPath)
                .branch(resolvedBranch)
                .commitSha(headCommitSha)
                .treeHash(treeHash)
                .fileManifest(fileHashes)
                .totalFiles((int) totalFiles)
                .snapshotTakenAt(Instant.now())
                .build();

        snapshot = repoSnapshotRepository.save(snapshot);
        log.info("Created RepoSnapshot {} (commit: {})", snapshot.getId(),
                headCommitSha != null ? headCommitSha.substring(0, Math.min(8, headCommitSha.length())) : "unknown");

        // Step 4: Trigger extraction run asynchronously
        triggerExtractionAsync(snapshot.getId(), workspaceId);

        return snapshot;
    }

    /**
     * Phase 3: Re-snapshot to detect drift.
     * Compares new tree hash with latest snapshot for this workspace.
     */
    @Transactional(readOnly = true)
    public SnapshotDiff detectDrift(UUID workspaceId, String localPath) {
        Path repoRoot = Path.of(localPath);
        Map<String, String> currentHashes = walkAndHash(repoRoot);
        String currentTreeHash = computeTreeHash(currentHashes);

        RepoSnapshot latest = repoSnapshotRepository.findLatestByWorkspaceId(workspaceId)
                .orElse(null);

        if (latest == null) {
            return new SnapshotDiff(true, "No previous snapshot", currentTreeHash, null, currentHashes.size(), 0);
        }

        boolean hasDrift = !latest.getTreeHash().equals(currentTreeHash);
        int newFiles = 0;
        int changedFiles = 0;

        if (hasDrift && latest.getFileManifest() != null) {
            for (Map.Entry<String, String> entry : currentHashes.entrySet()) {
                String prevHash = latest.getFileManifest().get(entry.getKey());
                if (prevHash == null) newFiles++;
                else if (!prevHash.equals(entry.getValue())) changedFiles++;
            }
        }

        return new SnapshotDiff(hasDrift,
                hasDrift ? (newFiles + " new files, " + changedFiles + " changed") : "No drift detected",
                currentTreeHash, latest.getTreeHash(), currentHashes.size(), latest.getTotalFiles());
    }

    @Async
    protected void triggerExtractionAsync(UUID snapshotId, UUID workspaceId) {
        log.info("Triggering extraction run for snapshot {}", snapshotId);
        extractionRunService.runExtraction(snapshotId, workspaceId);
    }

    // --- Internal helpers ---

    private Map<String, String> walkAndHash(Path root) {
        Map<String, String> hashes = new TreeMap<>(); // TreeMap = deterministic order
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    boolean tracked = TRACKED_EXTENSIONS.stream()
                            .anyMatch(ext -> name.endsWith(ext));
                    // Skip hidden dirs (.git, node_modules, target, .gradle)
                    if (tracked && !isSkippedPath(root, file)) {
                        try {
                            byte[] content = Files.readAllBytes(file);
                            String hash = sha256Hex(content);
                            String relativePath = root.relativize(file).toString()
                                    .replace('\\', '/');
                            hashes.put(relativePath, hash);
                        } catch (IOException e) {
                            log.warn("Cannot read file {}: {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (dirName.equals(".git") || dirName.equals("node_modules")
                            || dirName.equals("target") || dirName.equals(".gradle")
                            || dirName.equals("build") || dirName.equals("dist")
                            || dirName.equals(".next")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk repo: " + e.getMessage(), e);
        }
        return hashes;
    }

    private boolean isSkippedPath(Path root, Path file) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        return relative.startsWith(".git/") || relative.startsWith("node_modules/")
                || relative.startsWith("target/") || relative.startsWith(".gradle/")
                || relative.startsWith("build/") || relative.startsWith(".next/");
    }

    /**
     * Deterministic tree hash: SHA-256 of sorted "path:hash\n" lines.
     * Same files = same treeHash regardless of OS or scan order.
     */
    private String computeTreeHash(Map<String, String> fileHashes) {
        StringBuilder sb = new StringBuilder();
        // TreeMap guarantees sorted order → deterministic
        fileHashes.forEach((path, hash) -> sb.append(path).append(":").append(hash).append("\n"));
        return sha256Hex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String resolveHeadCommit(Path repoRoot) {
        try {
            Path headFile = repoRoot.resolve(".git/HEAD");
            if (!Files.exists(headFile)) return null;
            String headContent = Files.readString(headFile).trim();
            if (headContent.startsWith("ref: ")) {
                String refPath = headContent.substring(5).trim();
                Path refFile = repoRoot.resolve(".git").resolve(Path.of(refPath));
                if (Files.exists(refFile)) {
                    return Files.readString(refFile).trim();
                }
            }
            return headContent; // detached HEAD = commit SHA directly
        } catch (IOException e) {
            return null;
        }
    }

    private String resolveBranch(Path repoRoot) {
        try {
            Path headFile = repoRoot.resolve(".git/HEAD");
            if (!Files.exists(headFile)) return "unknown";
            String headContent = Files.readString(headFile).trim();
            if (headContent.startsWith("ref: refs/heads/")) {
                return headContent.substring("ref: refs/heads/".length()).trim();
            }
            return "detached";
        } catch (IOException e) {
            return "unknown";
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Drift detection result record.
     */
    public record SnapshotDiff(
            boolean hasDrift,
            String summary,
            String currentTreeHash,
            String previousTreeHash,
            int currentFileCount,
            int previousFileCount
    ) {}
}
