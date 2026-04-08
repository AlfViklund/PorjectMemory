package ai.productmemory.service;

import ai.productmemory.domain.entity.ChangeRequest;
import ai.productmemory.domain.entity.ImpactAnalysisOutput;
import ai.productmemory.domain.entity.Workspace;
import ai.productmemory.domain.enums.ChangeRequestStatus;
import ai.productmemory.repository.ChangeRequestRepository;
import ai.productmemory.repository.ImpactAnalysisOutputRepository;
import ai.productmemory.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * ChangeRequestService — Phase 6.
 *
 * GATE: CR cannot transition to IN_PLANNING without a valid ImpactAnalysisOutput (status=COMPLETED).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ImpactAnalysisOutputRepository impactAnalysisOutputRepository;
    private final ImpactAnalysisService impactAnalysisService;

    @Transactional
    public ChangeRequest createChangeRequest(UUID workspaceId, String title, String intent, String rationale) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        ChangeRequest cr = ChangeRequest.builder()
                .workspace(workspace)
                .shortId(generateShortId("cr"))
                .title(title)
                .intent(intent)
                .rationale(rationale)
                .status(ChangeRequestStatus.PROPOSED)
                .build();

        cr = changeRequestRepository.save(cr);
        log.info("Created Change Request {} ({})", cr.getShortId(), cr.getId());

        // Automatically trigger Impact Analysis
        triggerImpactAnalysis(cr.getId());

        return cr;
    }

    /**
     * Phase 6 GATE: Transition to IN_PLANNING is blocked unless ImpactAnalysis is COMPLETED.
     */
    @Transactional
    public ChangeRequest transitionToPlanning(UUID crId) {
        ChangeRequest changeRequest = changeRequestRepository.findByIdWithImpactAnalysis(crId)
                .orElseThrow(() -> new IllegalArgumentException("ChangeRequest not found: " + crId));

        ImpactAnalysisOutput analysis = impactAnalysisOutputRepository
                .findByChangeRequestId(crId)
                .orElseThrow(() -> new IllegalStateException(
                        "GATE BLOCKED: No ImpactAnalysisOutput exists for CR " + changeRequest.getShortId() +
                        ". Impact Analysis must be completed before planning."));

        if (!"COMPLETED".equals(analysis.getStatus())) {
            throw new IllegalStateException(
                    "GATE BLOCKED: ImpactAnalysis for CR " + changeRequest.getShortId() +
                    " is in status '" + analysis.getStatus() + "'. Must be COMPLETED before planning.");
        }

        changeRequest.setStatus(ChangeRequestStatus.IN_PLANNING);
        ChangeRequest saved = changeRequestRepository.save(changeRequest);
        log.info("CR {} transitioned to IN_PLANNING", saved.getShortId());
        return saved;
    }

    @Transactional
    public ChangeRequest updateStatus(UUID crId, ChangeRequestStatus newStatus) {
        ChangeRequest found = changeRequestRepository.findById(crId)
                .orElseThrow(() -> new IllegalArgumentException("CR not found: " + crId));
        ChangeRequestStatus old = found.getStatus();
        found.setStatus(newStatus);
        if (newStatus == ChangeRequestStatus.MERGED) {
            found.setMergedAt(Instant.now());
        }
        ChangeRequest saved = changeRequestRepository.save(found);
        log.info("CR {} status: {} -> {}", saved.getShortId(), old, newStatus);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequest> listByWorkspace(UUID workspaceId, Pageable pageable) {
        return changeRequestRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public ChangeRequest getById(UUID id) {
        return changeRequestRepository.findByIdWithImpactAnalysis(id)
                .orElseThrow(() -> new IllegalArgumentException("CR not found: " + id));
    }

    @Async
    protected void triggerImpactAnalysis(UUID crId) {
        log.info("Triggering Impact Analysis for CR {}", crId);
        impactAnalysisService.analyze(crId);
    }

    private String generateShortId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
