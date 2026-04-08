package ai.productmemory.domain.model;

import java.util.List;

public record CapabilityReportJson(
    List<String> buildCommands,
    List<String> testCommands,
    List<String> logLocations,
    List<String> envRequirements
) {}
