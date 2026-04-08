package ai.productmemory.api.controller;

import ai.productmemory.api.dto.ApiResponse;
import ai.productmemory.domain.entity.MemoryLink;
import ai.productmemory.domain.entity.ProductMemoryItem;
import ai.productmemory.domain.enums.MemoryItemStatus;
import ai.productmemory.domain.enums.ProductMemoryItemType;
import ai.productmemory.repository.MemoryLinkRepository;
import ai.productmemory.repository.ProductMemoryItemRepository;
import ai.productmemory.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/memory")
@RequiredArgsConstructor
public class ProductMemoryController {

    private final ProductMemoryItemRepository itemRepository;
    private final MemoryLinkRepository linkRepository;
    private final WorkspaceRepository workspaceRepository;

    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> listNodes(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<ProductMemoryItem> items;
        if (type != null) {
            try {
                ProductMemoryItemType itemType = ProductMemoryItemType.valueOf(type);
                List<ProductMemoryItem> filtered = itemRepository.findByWorkspaceIdAndItemType(workspaceId, itemType);
                items = new org.springframework.data.domain.PageImpl<>(
                        filtered, PageRequest.of(page, size), filtered.size());
            } catch (IllegalArgumentException e) {
                items = itemRepository.findByWorkspaceId(workspaceId, PageRequest.of(page, size));
            }
        } else {
            items = itemRepository.findByWorkspaceId(workspaceId, PageRequest.of(page, size));
        }

        return ResponseEntity.ok(ApiResponse.ok(items.map(this::toNodeSummary)));
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNode(
            @PathVariable UUID workspaceId,
            @PathVariable UUID nodeId) {
        ProductMemoryItem item = itemRepository.findByIdWithLinks(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Memory item not found: " + nodeId));

        List<MemoryLink> links = linkRepository.findAllRelatedLinks(nodeId);
        return ResponseEntity.ok(ApiResponse.ok(toNodeDetail(item, links)));
    }

    @GetMapping("/graph")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraph(
            @PathVariable UUID workspaceId) {
        List<ProductMemoryItem> allItems = itemRepository.findByWorkspaceId(
                workspaceId, PageRequest.of(0, 500)).getContent();

        List<Map<String, Object>> nodes = allItems.stream()
                .map(this::toNodeSummary).toList();

        List<Map<String, Object>> edges = new ArrayList<>();
        for (ProductMemoryItem item : allItems) {
            List<MemoryLink> outgoing = linkRepository.findBySourceItemId(item.getId());
            for (MemoryLink link : outgoing) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("id", link.getId().toString());
                edge.put("source", link.getSourceItem().getId().toString());
                edge.put("target", link.getTargetItem().getId().toString());
                edge.put("type", link.getLinkType().name());
                edge.put("confidence", link.getConfidence());
                edge.put("stale", link.getStale());
                edges.add(edge);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/nodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNode(
            @PathVariable UUID workspaceId,
            @RequestBody Map<String, Object> request) {

        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        ProductMemoryItem item = ProductMemoryItem.builder()
                .workspace(workspace)
                .itemType(ProductMemoryItemType.valueOf((String) request.get("type")))
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .status(MemoryItemStatus.PROPOSED)
                .confidence(0.5)
                .sourceType("manual")
                .build();

        item = itemRepository.save(item);
        return ResponseEntity.ok(ApiResponse.ok(toNodeSummary(item)));
    }

    private Map<String, Object> toNodeSummary(ProductMemoryItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId().toString());
        m.put("type", item.getItemType().name());
        m.put("name", item.getName());
        m.put("description", item.getDescription());
        m.put("status", item.getStatus().name());
        m.put("confidence", item.getConfidence());
        m.put("sourceType", item.getSourceType());
        m.put("groundingRef", item.getGroundingRef());
        m.put("createdAt", item.getCreatedAt());
        return m;
    }

    private Map<String, Object> toNodeDetail(ProductMemoryItem item, List<MemoryLink> links) {
        Map<String, Object> m = toNodeSummary(item);
        m.put("properties", item.getProperties());
        m.put("staleReason", item.getStaleReason());

        List<Map<String, Object>> outgoing = new ArrayList<>();
        List<Map<String, Object>> incoming = new ArrayList<>();

        for (MemoryLink l : links) {
            if (l.getSourceItem().getId().equals(item.getId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", l.getId().toString());
                e.put("type", l.getLinkType().name());
                e.put("targetId", l.getTargetItem().getId().toString());
                e.put("confidence", l.getConfidence());
                outgoing.add(e);
            } else {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", l.getId().toString());
                e.put("type", l.getLinkType().name());
                e.put("sourceId", l.getSourceItem().getId().toString());
                e.put("confidence", l.getConfidence());
                incoming.add(e);
            }
        }

        m.put("outgoingLinks", outgoing);
        m.put("incomingLinks", incoming);
        return m;
    }
}
