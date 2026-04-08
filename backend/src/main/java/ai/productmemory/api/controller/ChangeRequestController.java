package ai.productmemory.api.controller;

import ai.productmemory.api.dto.ApiResponse;
import ai.productmemory.api.dto.ChangeRequestDto;
import ai.productmemory.api.dto.ImpactAnalysisDto;
import ai.productmemory.domain.entity.ChangeRequest;
import ai.productmemory.domain.entity.ImpactAnalysisOutput;
import ai.productmemory.service.ChangeRequestService;
import ai.productmemory.service.ImpactAnalysisService;
import ai.productmemory.repository.ImpactAnalysisOutputRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;
    private final ImpactAnalysisOutputRepository impactAnalysisOutputRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ChangeRequestDto.Response>> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ChangeRequestDto.CreateRequest req) {
        ChangeRequest cr = changeRequestService.createChangeRequest(
                workspaceId, req.title(), req.intent(), req.rationale());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toResponse(cr)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ChangeRequestDto.ListItem>>> list(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ChangeRequest> result = changeRequestService.listByWorkspace(
                workspaceId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(result.map(this::toListItem)));
    }

    @GetMapping("/{crId}")
    public ResponseEntity<ApiResponse<ChangeRequestDto.Response>> get(
            @PathVariable UUID workspaceId,
            @PathVariable UUID crId) {
        ChangeRequest cr = changeRequestService.getById(crId);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(cr)));
    }

    @PostMapping("/{crId}/transition/planning")
    public ResponseEntity<ApiResponse<ChangeRequestDto.Response>> transitionToPlanning(
            @PathVariable UUID workspaceId,
            @PathVariable UUID crId) {
        ChangeRequest cr = changeRequestService.transitionToPlanning(crId);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(cr)));
    }

    @GetMapping("/{crId}/impact-analysis")
    public ResponseEntity<ApiResponse<ImpactAnalysisDto.Full>> getImpactAnalysis(
            @PathVariable UUID workspaceId,
            @PathVariable UUID crId) {
        ImpactAnalysisOutput ia = impactAnalysisOutputRepository.findByChangeRequestId(crId)
                .orElseThrow(() -> new IllegalArgumentException("No Impact Analysis found for CR: " + crId));
        return ResponseEntity.ok(ApiResponse.ok(toImpactFull(ia)));
    }

    private ChangeRequestDto.Response toResponse(ChangeRequest cr) {
        ImpactAnalysisDto.Summary iaSummary = null;
        if (cr.getImpactAnalysis() != null) {
            ImpactAnalysisOutput ia = cr.getImpactAnalysis();
            iaSummary = new ImpactAnalysisDto.Summary(ia.getId(), ia.getShortId(),
                    ia.getStatus(), ia.getSchemaVersion(), ia.getCreatedAt(), ia.getCompletedAt());
        }
        return new ChangeRequestDto.Response(
                cr.getId(), cr.getShortId(), cr.getWorkspace().getId(),
                cr.getTitle(), cr.getIntent(), cr.getRationale(),
                cr.getStatus(), cr.getPriority(),
                cr.getCreatedAt(), cr.getUpdatedAt(), cr.getMergedAt(),
                iaSummary
        );
    }

    private ChangeRequestDto.ListItem toListItem(ChangeRequest cr) {
        String iaStatus = cr.getImpactAnalysis() != null ? cr.getImpactAnalysis().getStatus() : "NOT_STARTED";
        return new ChangeRequestDto.ListItem(
                cr.getId(), cr.getShortId(), cr.getTitle(),
                cr.getStatus(), cr.getPriority(), iaStatus, cr.getCreatedAt()
        );
    }

    private ImpactAnalysisDto.Full toImpactFull(ImpactAnalysisOutput ia) {
        return new ImpactAnalysisDto.Full(
                ia.getId(), ia.getShortId(), ia.getChangeRequest().getId(),
                ia.getStatus(), ia.getSchemaVersion(), ia.getPayload(),
                ia.getErrorMessage(), ia.getCreatedAt(), ia.getCompletedAt()
        );
    }
}
