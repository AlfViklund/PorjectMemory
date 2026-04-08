package ai.productmemory.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ImpactAnalysisDto {

    public record Summary(
            UUID id,
            String shortId,
            String status,
            String schemaVersion,
            Instant createdAt,
            Instant completedAt
    ) {}

    public record Full(
            UUID id,
            String shortId,
            UUID changeRequestId,
            String status,
            String schemaVersion,
            Map<String, Object> payload,
            String errorMessage,
            Instant createdAt,
            Instant completedAt
    ) {}
}
