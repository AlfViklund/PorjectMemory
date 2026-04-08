package ai.productmemory.api.dto;

import ai.productmemory.domain.enums.ChangeRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ChangeRequestDto {

    public record CreateRequest(
            @NotBlank @Size(max = 512) String title,
            @NotBlank String intent,
            String rationale
    ) {}

    public record Response(
            UUID id,
            String shortId,
            UUID workspaceId,
            String title,
            String intent,
            String rationale,
            ChangeRequestStatus status,
            Integer priority,
            Instant createdAt,
            Instant updatedAt,
            Instant mergedAt,
            ImpactAnalysisDto.Summary impactAnalysis
    ) {}

    public record ListItem(
            UUID id,
            String shortId,
            String title,
            ChangeRequestStatus status,
            Integer priority,
            String impactAnalysisStatus,
            Instant createdAt
    ) {}
}
