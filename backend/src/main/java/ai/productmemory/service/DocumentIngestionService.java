package ai.productmemory.service;

import ai.productmemory.domain.entity.SourceAsset;
import ai.productmemory.domain.entity.SourceSection;
import ai.productmemory.domain.entity.Workspace;
import ai.productmemory.domain.enums.SourceAssetType;
import ai.productmemory.repository.SourceAssetRepository;
import ai.productmemory.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DocumentIngestionService — Phase 2 implementation.
 *
 * Pipeline:
 * 1. Upload asset (save to S3/MinIO)
 * 2. Extract raw text
 * 3. Section the document (heading-based)
 * 4. [Async] Trigger requirement extraction & capability synthesis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final SourceAssetRepository sourceAssetRepository;
    private final WorkspaceRepository workspaceRepository;
    private final StorageService storageService;
    private final RequirementExtractionService requirementExtractionService;

    private static final Pattern HEADING_PATTERN =
            Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    /**
     * Phase 2 Step 1: Upload and register asset.
     * Returns the persisted SourceAsset (not yet processed).
     */
    @Transactional
    public SourceAsset uploadAsset(UUID workspaceId, MultipartFile file, SourceAssetType assetType) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        String storageKey = "workspaces/" + workspaceId + "/assets/" + UUID.randomUUID() + "/" + file.getOriginalFilename();

        try {
            byte[] bytes = file.getBytes();
            String contentHash = sha256Hex(bytes);

            // Idempotency: skip if same content already exists
            Optional<SourceAsset> existing = sourceAssetRepository
                    .findByWorkspaceIdAndContentHash(workspaceId, contentHash);
            if (existing.isPresent()) {
                log.info("Asset with same content already exists: {}", existing.get().getId());
                return existing.get();
            }

            // Upload to S3/MinIO
            storageService.upload(storageKey, bytes, file.getContentType());

            SourceAsset asset = SourceAsset.builder()
                    .workspace(workspace)
                    .fileName(file.getOriginalFilename())
                    .assetType(assetType)
                    .storageKey(storageKey)
                    .sizeBytes(file.getSize())
                    .contentHash(contentHash)
                    .mimeType(file.getContentType())
                    .processed(false)
                    .build();

            asset = sourceAssetRepository.save(asset);
            log.info("Uploaded asset {} for workspace {}", asset.getId(), workspaceId);

            // Trigger async processing
            processAssetAsync(asset.getId());

            return asset;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload asset: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 2 Step 2+3: Extract text and section the document.
     * Runs asynchronously after upload.
     */
    @Async
    @Transactional
    public void processAssetAsync(UUID assetId) {
        log.info("Starting async processing for asset {}", assetId);
        SourceAsset asset = sourceAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));

        try {
            // Step 2: Extract raw text
            byte[] bytes = storageService.download(asset.getStorageKey());
            String rawText = extractText(bytes, asset.getAssetType());
            asset.setRawText(rawText);

            // Step 3: Section the document
            List<SourceSection> sections = sectionDocument(rawText, asset);
            asset.setSections(sections);
            asset.setProcessed(true);

            sourceAssetRepository.save(asset);
            log.info("Processed asset {}: {} sections extracted", assetId, sections.size());

            // Step 4: Trigger requirement extraction (separate async pipeline)
            requirementExtractionService.extractFromAsset(assetId);

        } catch (Exception e) {
            log.error("Failed to process asset {}: {}", assetId, e.getMessage(), e);
        }
    }

    /**
     * Phase 2 Step 3: Section document by headings.
     */
    private List<SourceSection> sectionDocument(String rawText, SourceAsset asset) {
        List<SourceSection> sections = new ArrayList<>();

        if (asset.getAssetType() == SourceAssetType.MARKDOWN) {
            sections = sectionMarkdown(rawText, asset);
        } else {
            // For other types: treat as single section
            SourceSection section = SourceSection.builder()
                    .sourceAsset(asset)
                    .content(rawText)
                    .sectionIndex(0)
                    .headingLevel(0)
                    .build();
            sections.add(section);
        }

        return sections;
    }

    private List<SourceSection> sectionMarkdown(String rawText, SourceAsset asset) {
        List<SourceSection> sections = new ArrayList<>();
        String[] lines = rawText.split("\n");
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = null;
        int currentLevel = 0;
        int sectionIndex = 0;
        long currentOffset = 0;
        long sectionStart = 0;

        for (String line : lines) {
            Matcher m = HEADING_PATTERN.matcher(line);
            if (m.matches()) {
                // Save previous section
                if (currentContent.length() > 0) {
                    sections.add(SourceSection.builder()
                            .sourceAsset(asset)
                            .title(currentTitle)
                            .content(currentContent.toString().trim())
                            .sectionIndex(sectionIndex++)
                            .headingLevel(currentLevel)
                            .startOffset(sectionStart)
                            .endOffset(currentOffset)
                            .build());
                    currentContent = new StringBuilder();
                }
                currentTitle = m.group(2).trim();
                currentLevel = m.group(1).length();
                sectionStart = currentOffset;
            } else {
                currentContent.append(line).append("\n");
            }
            currentOffset += line.length() + 1;
        }

        // Last section
        if (currentContent.length() > 0) {
            sections.add(SourceSection.builder()
                    .sourceAsset(asset)
                    .title(currentTitle)
                    .content(currentContent.toString().trim())
                    .sectionIndex(sectionIndex)
                    .headingLevel(currentLevel)
                    .startOffset(sectionStart)
                    .endOffset(currentOffset)
                    .build());
        }

        return sections;
    }

    private String extractText(byte[] bytes, SourceAssetType type) {
        // For MVP: support plain text and markdown directly
        // PDF/DOCX extraction requires additional libraries (added in Phase 2 extension)
        return new String(bytes, StandardCharsets.UTF_8);
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
}
