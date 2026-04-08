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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public RuntimeCapabilityReport runProbe(UUID workspaceId) {
        log.info("Starting capability probe for workspace {}", workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        Path rootPath = Paths.get(workspace.getDirectoryPath());
        
        List<String> buildCommands = new ArrayList<>();
        List<String> testCommands = new ArrayList<>();
        List<String> logLocations = new ArrayList<>();
        List<String> envRequirements = new ArrayList<>();

        // Heuristic: Java Maven Project
        if (Files.exists(rootPath.resolve("pom.xml")) || Files.exists(rootPath.resolve("backend/pom.xml"))) {
            log.info("Probe detected Maven project.");
            buildCommands.add("mvn clean install -DskipTests");
            testCommands.add("mvn test");
            envRequirements.add("Java 21");
            envRequirements.add("Maven 3.9+");
        }

        // Heuristic: Node Project
        if (Files.exists(rootPath.resolve("package.json")) || Files.exists(rootPath.resolve("frontend/package.json"))) {
            log.info("Probe detected Node/NPM project.");
            buildCommands.add("npm install && npm run build");
            testCommands.add("npm test");
            envRequirements.add("Node.js 20+");
        }
        
        // Dummy log locations
        logLocations.add("logs/application.log");

        CapabilityReportJson reportJson = new CapabilityReportJson(
                buildCommands,
                testCommands,
                logLocations,
                envRequirements
        );

        RuntimeCapabilityReport report = new RuntimeCapabilityReport()
                .setWorkspace(workspace)
                .setStatus(CapabilityReportStatus.GENERATED)
                .setReportJsonb(reportJson);

        return reportRepository.save(report);
    }

    @Transactional
    public RuntimeCapabilityReport approveReport(UUID reportId) {
        RuntimeCapabilityReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Capability Report not found: " + reportId));
        
        report.setStatus(CapabilityReportStatus.APPROVED);
        return reportRepository.save(report);
    }
}
