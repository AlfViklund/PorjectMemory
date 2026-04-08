package ai.productmemory.api.controller;

import ai.productmemory.domain.entity.RuntimeCapabilityReport;
import ai.productmemory.service.CapabilityResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/runtime")
@RequiredArgsConstructor
public class RuntimeMonitorController {

    private final CapabilityResearchService researchService;

    @PostMapping("/probe")
    public ResponseEntity<RuntimeCapabilityReport> runProbe(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(researchService.runProbe(workspaceId));
    }

    @PostMapping("/reports/{reportId}/approve")
    public ResponseEntity<RuntimeCapabilityReport> approveReport(
            @PathVariable UUID workspaceId,
            @PathVariable UUID reportId) {
        return ResponseEntity.ok(researchService.approveReport(reportId));
    }
}
