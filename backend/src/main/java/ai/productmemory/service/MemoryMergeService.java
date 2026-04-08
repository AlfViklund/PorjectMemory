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
 * MemoryMergeService — Phase 10.
 * Memory CANNOT be updated implicitly. Ever.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryMergeService {

    private final ReviewRepository reviewRepository;
    private final MemoryMergePlanRepository mergePlanRepository;
    private final ProductMemoryItemRepository memoryItemRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public MemoryMergePlan generateMergePlan(UUID reviewId, List<Map<String, Object>> proposedChanges) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        if (review.getStatus() != ReviewStatus.APPROVED && review.getStatus() != ReviewStatus.PARTIALLY_APPROVED) {
            throw new IllegalStateException(
                    "Cannot generate MergePlan: Review " + review.getShortId() +
                    " is not approved. Current status: " + review.getStatus());
        }

        if (mergePlanRepository.findByReviewId(reviewId).isPresent()) {
            throw new IllegalStateException("MemoryMergePlan already exists for review " + review.getShortId());
        }

        String shortId = "mm-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map<String, Object> payload = buildMergePlanPayload(proposedChanges);

        MemoryMergePlan plan = MemoryMergePlan.builder()
                .shortId(shortId)
                .review(review)
                .schemaVersion("1.0")
                .status(MergePlanStatus.PENDING_REVIEW)
                .payload(payload)
                .suggestedFollowUps(Map.of("items", List.of()))
                .build();

        plan = mergePlanRepository.save(plan);
        log.info("Generated MemoryMergePlan {} for review {}", shortId, review.getShortId());
        return plan;
    }

    @Transactional
    public MemoryMergePlan approveAndApply(UUID planId, String reviewerId,
                                            List<String> approvedItemIds,
                                            List<String> rejectedItemIds) {
        MemoryMergePlan plan = mergePlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("MemoryMergePlan not found: " + planId));

        if (plan.getStatus() != MergePlanStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Plan " + plan.getShortId() + " is in status " + plan.getStatus() + ", cannot approve.");
        }

        plan.setReviewerId(reviewerId);
        boolean isPartial = rejectedItemIds != null && !rejectedItemIds.isEmpty();
        plan.setStatus(isPartial ? MergePlanStatus.PARTIALLY_APPROVED : MergePlanStatus.APPROVED);
        plan.setApprovedAt(Instant.now());

        Map<String, Object> decisions = new HashMap<>();
        decisions.put("approvedItems", approvedItemIds != null ? approvedItemIds : List.of());
        decisions.put("rejectedItems", rejectedItemIds != null ? rejectedItemIds : List.of());
        plan.setApprovalDecisions(decisions);

        log.info("Plan {} approved ({}) by {}", plan.getShortId(), plan.getStatus(), reviewerId);

        try {
            applyChangesAtomically(plan, approvedItemIds);
            plan.setStatus(MergePlanStatus.APPLIED);
            plan.setAppliedAt(Instant.now());
            log.info("Plan {} applied atomically", plan.getShortId());
        } catch (Exception e) {
            plan.setStatus(MergePlanStatus.FAILED);
            plan.setApplicationError(e.getMessage());
            log.error("Plan {} application failed: {}", plan.getShortId(), e.getMessage(), e);
            throw e;
        }

        // Update CR status to MERGED
        ChangeRequest cr = plan.getReview().getChangeRequest();
        cr.setStatus(ChangeRequestStatus.MERGED);
        cr.setMergedAt(Instant.now());

        return mergePlanRepository.save(plan);
    }

    private void applyChangesAtomically(MemoryMergePlan plan, List<String> approvedItemIds) {
        Map<String, Object> payload = plan.getPayload();
        if (payload == null) return;

        Object changesToApplyObj = payload.get("changesToApply");
        if (!(changesToApplyObj instanceof Map<?, ?> changesToApply)) return;

        // Apply CREATES
        Object createsObj = changesToApply.get("create");
        if (createsObj instanceof List<?> createsList) {
            UUID workspaceId = plan.getReview().getChangeRequest().getWorkspace().getId();
            for (Object item : createsList) {
                if (item instanceof Map<?, ?> itemMap) {
                    String itemId = (String) itemMap.get("id");
                    if (isApproved(itemId, approvedItemIds)) {
                        applyCreate(itemMap, workspaceId);
                    }
                }
            }
        }

        // Apply MARK AS STALE
        Object staleObj = changesToApply.get("markAsStale");
        if (staleObj instanceof List<?> staleList) {
            for (Object item : staleList) {
                if (item instanceof Map<?, ?> itemMap) {
                    String itemId = (String) itemMap.get("id");
                    if (isApproved(itemId, approvedItemIds)) {
                        applyMarkStale(itemMap);
                    }
                }
            }
        }
    }

    private void applyCreate(Map<?, ?> item, UUID workspaceId) {
        String type = (String) item.get("type");
        if (type == null) return;

        try {
            ProductMemoryItemType itemType = ProductMemoryItemType.valueOf(type);
            Workspace workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

            String name = item.get("name") instanceof String s ? s : "Unnamed";
            String description = item.get("description") instanceof String d ? d : null;

            ProductMemoryItem newItem = ProductMemoryItem.builder()
                    .workspace(workspace)
                    .itemType(itemType)
                    .name(name)
                    .description(description)
                    .status(MemoryItemStatus.FRESH)
                    .confidence(1.0)
                    .sourceType("memory_merge")
                    .build();

            memoryItemRepository.save(newItem);
            log.info("Created memory item: {} ({})", newItem.getName(), type);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown item type in merge plan: {}", type);
        }
    }

    private void applyMarkStale(Map<?, ?> item) {
        String idStr = (String) item.get("id");
        if (idStr == null) return;
        try {
            UUID id = UUID.fromString(idStr);
            String reason = item.get("reason") instanceof String r ? r : "Marked stale by MemoryMergePlan";
            memoryItemRepository.findById(id).ifPresent(mi -> {
                mi.setStatus(MemoryItemStatus.STALE);
                mi.setStaleReason(reason);
                memoryItemRepository.save(mi);
                log.info("Marked memory item {} as STALE", id);
            });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in markAsStale: {}", idStr);
        }
    }

    private boolean isApproved(String itemId, List<String> approvedIds) {
        if (approvedIds == null || approvedIds.isEmpty()) return true;
        return approvedIds.contains(itemId);
    }

    private Map<String, Object> buildMergePlanPayload(List<Map<String, Object>> proposedChanges) {
        Map<String, Object> changesToApply = new LinkedHashMap<>();
        changesToApply.put("create", proposedChanges != null ? proposedChanges : List.of());
        changesToApply.put("update", List.of());
        changesToApply.put("markAsStale", List.of());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", "1.0");
        payload.put("changesToApply", changesToApply);
        payload.put("potentialConflicts", List.of());
        return payload;
    }
}
