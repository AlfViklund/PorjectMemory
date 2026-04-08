package ai.productmemory.service;

import ai.productmemory.domain.entity.TaskContextPack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * TaskContextPackValidationService — Phase 4.
 *
 * Validates a TaskContextPack against the strict JSON Schema defined in spec.
 * Core fields: taskIdentity, linkedChangeRequest, summary, rationale,
 * acceptanceCriteria, impactedProductAreas, scope, runtimeContext,
 * postExecutionMemoryUpdatePlan must all be present and non-empty.
 *
 * Extension in 'extensions' field is allowed, but core is closed.
 */
@Service
@Slf4j
public class TaskContextPackValidationService {

    private static final List<String> REQUIRED_CORE_FIELDS = List.of(
            "taskIdentity",
            "linkedChangeRequest",
            "summary",
            "rationale",
            "acceptanceCriteria",
            "impactedProductAreas",
            "scope",
            "runtimeContext",
            "postExecutionMemoryUpdatePlan"
    );

    /**
     * Validates presence of all required core fields.
     * Returns true if valid, false if any required field is missing or empty.
     */
    public boolean validate(TaskContextPack pack) {
        if (pack.getPayload() == null) {
            log.warn("TaskContextPack {} has null payload", pack.getShortId());
            return false;
        }

        Map<String, Object> payload = pack.getPayload();

        for (String field : REQUIRED_CORE_FIELDS) {
            if (!payload.containsKey(field) || payload.get(field) == null) {
                log.warn("TaskContextPack {} missing required field: {}", pack.getShortId(), field);
                return false;
            }
            Object value = payload.get(field);
            if (value instanceof String s && s.isBlank()) {
                log.warn("TaskContextPack {} has blank field: {}", pack.getShortId(), field);
                return false;
            }
        }

        // Check for unresolved conflicts
        if (Boolean.TRUE.equals(pack.getHasUnresolvedConflicts())) {
            log.warn("TaskContextPack {} has unresolved conflicts — execution blocked", pack.getShortId());
            return false;
        }

        log.info("TaskContextPack {} is valid", pack.getShortId());
        return true;
    }

    /**
     * Validates that runtime adapter receives a valid pack before execution.
     * This is the GATE called by the runtime adapter.
     */
    public void assertValidForExecution(TaskContextPack pack) {
        if (pack == null) {
            throw new IllegalStateException(
                    "EXECUTION BLOCKED: No TaskContextPack provided. Runtime adapter cannot proceed without it.");
        }
        if (!"VALID".equals(pack.getValidationStatus())) {
            throw new IllegalStateException(
                    "EXECUTION BLOCKED: TaskContextPack " + pack.getShortId() +
                    " has validation status: " + pack.getValidationStatus() +
                    ". Only VALID packs can be submitted to the runtime adapter.");
        }
        if (Boolean.TRUE.equals(pack.getHasUnresolvedConflicts())) {
            throw new IllegalStateException(
                    "EXECUTION BLOCKED: TaskContextPack " + pack.getShortId() +
                    " has unresolved conflicts. Resolve conflicts before execution.");
        }
    }
}
