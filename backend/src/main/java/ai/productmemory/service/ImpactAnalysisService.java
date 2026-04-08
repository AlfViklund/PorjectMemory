package ai.productmemory.service;

import ai.productmemory.domain.entity.ChangeRequest;
import ai.productmemory.domain.entity.ImpactAnalysisOutput;
import ai.productmemory.domain.entity.ProductMemoryItem;
import ai.productmemory.domain.enums.ChangeRequestStatus;
import ai.productmemory.repository.ChangeRequestRepository;
import ai.productmemory.repository.ImpactAnalysisOutputRepository;
import ai.productmemory.repository.ProductMemoryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * ImpactAnalysisService — Phase 6.
 *
 * Generates a persisted ImpactAnalysisOutput artifact for a ChangeRequest.
 * This is an OBLIGATORY artifact and a GATE for planning.
 *
 * MVP implementation: keyword-based heuristic analysis against Product Memory.
 * V2+: LLM-based deep analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImpactAnalysisService {

    private final ChangeRequestRepository changeRequestRepository;
    private final ImpactAnalysisOutputRepository impactAnalysisOutputRepository;
    private final ProductMemoryItemRepository productMemoryItemRepository;

    @Async
    @Transactional
    public void analyze(UUID crId) {
        ChangeRequest cr = changeRequestRepository.findById(crId)
                .orElseThrow(() -> new IllegalArgumentException("CR not found: " + crId));

        // Create initial PENDING artifact
        String shortId = "ia-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        ImpactAnalysisOutput analysis = ImpactAnalysisOutput.builder()
                .shortId(shortId)
                .changeRequest(cr)
                .schemaVersion("1.0")
                .status("RUNNING")
                .payload(new HashMap<>())
                .build();

        analysis = impactAnalysisOutputRepository.save(analysis);
        cr.setStatus(ChangeRequestStatus.IMPACT_ANALYSIS_PENDING);
        changeRequestRepository.save(cr);

        log.info("Started Impact Analysis {} for CR {}", shortId, cr.getShortId());

        try {
            // Heuristic impact analysis against Product Memory items
            Map<String, Object> payload = performAnalysis(cr);

            analysis.setPayload(payload);
            analysis.setStatus("COMPLETED");
            analysis.setCompletedAt(Instant.now());
            impactAnalysisOutputRepository.save(analysis);

            cr.setStatus(ChangeRequestStatus.IMPACT_ANALYSIS_COMPLETE);
            changeRequestRepository.save(cr);

            log.info("Completed Impact Analysis {} for CR {}", shortId, cr.getShortId());

        } catch (Exception e) {
            analysis.setStatus("FAILED");
            analysis.setErrorMessage(e.getMessage());
            impactAnalysisOutputRepository.save(analysis);
            log.error("Impact Analysis failed for CR {}: {}", cr.getShortId(), e.getMessage(), e);
        }
    }

    private Map<String, Object> performAnalysis(ChangeRequest cr) {
        String intent = cr.getIntent().toLowerCase();
        List<ProductMemoryItem> allItems = productMemoryItemRepository
                .findByWorkspaceIdAndItemTypeIn(cr.getWorkspace().getId(), List.of(
                        ai.productmemory.domain.enums.ProductMemoryItemType.CAPABILITY,
                        ai.productmemory.domain.enums.ProductMemoryItemType.API_ENDPOINT,
                        ai.productmemory.domain.enums.ProductMemoryItemType.SCREEN,
                        ai.productmemory.domain.enums.ProductMemoryItemType.DATA_ENTITY
                ));

        List<Map<String, Object>> impactedCapabilities = new ArrayList<>();
        List<Map<String, Object>> impactedScreens = new ArrayList<>();
        List<Map<String, Object>> impactedApis = new ArrayList<>();

        for (ProductMemoryItem item : allItems) {
            boolean matches = item.getName().toLowerCase().contains(extractKeyword(intent))
                    || (item.getDescription() != null &&
                        item.getDescription().toLowerCase().contains(extractKeyword(intent)));

            if (matches) {
                Map<String, Object> ref = Map.of(
                        "id", item.getId().toString(),
                        "name", item.getName(),
                        "confidence", 0.7
                );
                switch (item.getItemType()) {
                    case CAPABILITY -> impactedCapabilities.add(ref);
                    case SCREEN -> impactedScreens.add(ref);
                    case API_ENDPOINT -> impactedApis.add(ref);
                    default -> {}
                }
            }
        }

        return Map.of(
                "schemaVersion", "1.0",
                "impactedCapabilities", impactedCapabilities,
                "impactedScreens", impactedScreens,
                "impactedAPIs", impactedApis,
                "suspectedDependencyConflicts", List.of(),
                "recommendedExecutionScope", Map.of("tasks", List.of(), "testSuites", List.of()),
                "recommendedReviewScope", Map.of("memoryItems", List.of(), "screens", List.of()),
                "findingsGenerated", List.of(),
                "analysisMode", "heuristic_mvp"
        );
    }

    private String extractKeyword(String intent) {
        // Extract a meaningful keyword from the intent for heuristic matching
        String[] words = intent.split("\\s+");
        for (String word : words) {
            if (word.length() > 4 && !Set.of("should","would","could","change","update","create","delete").contains(word)) {
                return word;
            }
        }
        return words.length > 0 ? words[0] : "";
    }
}
