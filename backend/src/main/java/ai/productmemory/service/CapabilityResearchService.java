package ai.productmemory.service;

import ai.productmemory.domain.entity.CapabilityReportStatus;
import ai.productmemory.domain.entity.RuntimeCapabilityReport;
import ai.productmemory.domain.entity.Workspace;
import ai.productmemory.domain.model.CapabilityReportJson;
import ai.productmemory.repository.RuntimeCapabilityReportRepository;
import ai.productmemory.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapabilityResearchService {

    private final WorkspaceRepository workspaceRepository;
    private final RuntimeCapabilityReportRepository reportRepository;

    public RuntimeCapabilityReport runProbe(UUID workspaceId) {
        log.info("Starting capability probe for Workspace ID: {}", workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        String workspacePath = "C:\\Users\\Arslek\\Projects\\ProjectMemory";
        Path path = Paths.get(workspacePath);

        boolean hasPom = Files.exists(path.resolve("backend/pom.xml")) || Files.exists(path.resolve("pom.xml"));
        boolean hasPackageJson = Files.exists(path.resolve("frontend/package.json")) || Files.exists(path.resolve("package.json"));

        List<String> buildCommands = new ArrayList<>();
        List<String> testCommands = new ArrayList<>();
        List<String> detectedEnvs = new ArrayList<>();

        if (hasPom) {
            buildCommands.add("mvn clean install -DskipTests");
            testCommands.add("mvn test");
            detectedEnvs.add("java-maven");
        }

        if (hasPackageJson) {
            buildCommands.add("npm install && npm run build");
            testCommands.add("npm test");
            detectedEnvs.add("node-npm");
        }

        CapabilityReportJson reportJson = new CapabilityReportJson(
                detectedEnvs,
                buildCommands,
                testCommands,
                "Auto-detected by Phase 8 Probe"
        );

        RuntimeCapabilityReport report = RuntimeCapabilityReport.builder()
                .workspace(workspace)
                .reportJsonb(reportJson)
                .status(CapabilityReportStatus.GENERATED)
                .build();

        report = reportRepository.save(report);

        log.info("Probe finished. Environments detected: {}", detectedEnvs);
        return report;
    }

    public RuntimeCapabilityReport approveReport(UUID reportId) {
        RuntimeCapabilityReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        
        report.setStatus(CapabilityReportStatus.APPROVED);
        return reportRepository.save(report);
    }
}
