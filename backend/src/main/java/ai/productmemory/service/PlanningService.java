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
 * PlanningService — Phase 7: Grounded Planning.
 * TaskContextPack is created STRICT — stored as persisted artifact BEFORE task can execute.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningService {

    private final ChangeRequestRepository changeRequestRepository;
    private final TaskRepository taskRepository;
    private final TaskContextPackRepository taskContextPackRepository;
    private final ImpactAnalysisOutputRepository impactAnalysisOutputRepository;
    private final TaskContextPackValidationService validationService;

    @Transactional
    public Task createTaskWithContextPack(UUID crId, String taskTitle, String taskRationale,
                                          List<Map<String, Object>> acceptanceCriteria,
                                          String scope, String outOfScope) {
        ChangeRequest cr = changeRequestRepository.findByIdWithImpactAnalysis(crId)
                .orElseThrow(() -> new IllegalArgumentException("CR not found: " + crId));

        if (!EnumSet.of(ChangeRequestStatus.IN_PLANNING, ChangeRequestStatus.IMPACT_ANALYSIS_COMPLETE)
                .contains(cr.getStatus())) {
            throw new IllegalStateException(
                    "Cannot plan task: CR " + cr.getShortId() + " is in status " + cr.getStatus());
        }

        ImpactAnalysisOutput analysis = impactAnalysisOutputRepository
                .findByChangeRequestId(crId)
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .orElseThrow(() -> new IllegalStateException(
                        "GATE BLOCKED: TaskContextPack cannot be created without completed ImpactAnalysis."));

        String taskShortId = "task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Task task = Task.builder()
                .changeRequest(cr)
                .shortId(taskShortId)
                .title(taskTitle)
                .rationale(taskRationale)
                .status(TaskStatus.AWAITING_CONTEXT_PACK)
                .build();
        task = taskRepository.save(task);

        String tcpShortId = "tcp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map<String, Object> payload = buildContextPackPayload(tcpShortId, cr, task, analysis,
                acceptanceCriteria, scope, outOfScope);

        TaskContextPack contextPack = TaskContextPack.builder()
                .shortId(tcpShortId)
                .task(task)
                .schemaVersion("1.0")
                .validationStatus("PENDING")
                .payload(payload)
                .build();

        contextPack = taskContextPackRepository.save(contextPack);

        boolean valid = validationService.validate(contextPack);

        if (valid) {
            contextPack.setValidationStatus("VALID");
            contextPack.setValidatedAt(Instant.now());
            task.setStatus(TaskStatus.READY);
            task.setContextPackValidatedAt(Instant.now());
        } else {
            contextPack.setValidationStatus("INVALID");
            task.setStatus(TaskStatus.AWAITING_CONTEXT_PACK);
        }

        taskContextPackRepository.save(contextPack);
        task = taskRepository.save(task);

        log.info("Created Task {} with ContextPack {} (status: {})",
                task.getShortId(), contextPack.getShortId(), contextPack.getValidationStatus());
        return task;
    }

    private Map<String, Object> buildContextPackPayload(String shortId, ChangeRequest cr, Task task,
                                                         ImpactAnalysisOutput analysis,
                                                         List<Map<String, Object>> acceptanceCriteria,
                                                         String scope, String outOfScope) {
        Map<String, Object> runtimeContext = new LinkedHashMap<>();
        runtimeContext.put("repository", cr.getWorkspace().getPrimaryRepoUrl());
        runtimeContext.put("branch", "main");
        runtimeContext.put("dependencies", List.of());
        runtimeContext.put("environmentVariables", Map.of());

        Map<String, Object> postExecPlan = new LinkedHashMap<>();
        postExecPlan.put("createNewItems", List.of());
        postExecPlan.put("updateExistingLinks", List.of());
        postExecPlan.put("markAsStale", List.of());

        // Extract impacted areas from analysis payload safely
        Object impactedCapabilities = List.of();
        if (analysis.getPayload() instanceof Map<?, ?> analysisPayload) {
            Object caps = analysisPayload.get("impactedCapabilities");
            if (caps instanceof List<?>) {
                impactedCapabilities = caps;
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", "1.0");
        payload.put("taskIdentity", task.getShortId());
        payload.put("linkedChangeRequest", cr.getShortId());
        payload.put("summary", task.getTitle());
        payload.put("rationale", task.getRationale() != null ? task.getRationale() : cr.getIntent());
        payload.put("acceptanceCriteria", acceptanceCriteria != null ? acceptanceCriteria : List.of());
        payload.put("impactedProductAreas", impactedCapabilities);
        payload.put("scope", scope != null ? scope : "");
        payload.put("outOfScope", outOfScope != null ? outOfScope : "");
        payload.put("expectedDeliverables", List.of());
        payload.put("runtimeContext", runtimeContext);
        payload.put("postExecutionMemoryUpdatePlan", postExecPlan);
        payload.put("linkedImpactAnalysisId", analysis.getShortId());
        return payload;
    }
}
