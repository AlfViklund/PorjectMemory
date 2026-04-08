package ai.productmemory.service;

import ai.productmemory.domain.entity.*;
import ai.productmemory.domain.enums.ExecutionRunStatus;
import ai.productmemory.domain.enums.TaskStatus;
import ai.productmemory.repository.ExecutionRunRepository;
import ai.productmemory.repository.RuntimeCapabilityReportRepository;
import ai.productmemory.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionRunService {

    private final TaskRepository taskRepository;
    private final RuntimeCapabilityReportRepository reportRepository;
    private final ExecutionRunRepository executionRunRepository;
    private final DockerExecutionAdapter dockerAdapter;

    @Transactional
    public ExecutionRun startExecution(UUID taskId) {
        log.info("Attempting to start execution for task {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        UUID workspaceId = task.getChangeRequest().getWorkspace().getId();

        // STOP-GATE ENFORCEMENT
        RuntimeCapabilityReport report = reportRepository.findTopByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new IllegalStateException(
                        "STOP-GATE: No Capability Report found. You must run Capability Probe first."
                ));

        if (report.getStatus() != CapabilityReportStatus.APPROVED) {
            throw new IllegalStateException(
                    "STOP-GATE: Capability Report is not APPROVED (current status: " + report.getStatus() + ")."
            );
        }

        // PHASE 9: Docker Execution
        String image = String.valueOf(report.getReportJsonb().buildCommands()).contains("npm") ? "node:20-alpine" : "maven:3.9-eclipse-temurin-21";
        
        String command = "echo 'No commands found'";
        if (report.getReportJsonb().buildCommands() != null && !report.getReportJsonb().buildCommands().isEmpty()) {
            command = report.getReportJsonb().buildCommands().get(0);
        }

        // Create Run Entity
        ExecutionRun run = new ExecutionRun();
        run.setTask(task);
        run.setStatus(ExecutionRunStatus.RUNNING);
        run.setDockerImage(image);
        run.setStartedAt(Instant.now());
        
        run = executionRunRepository.save(run);
        
        String workspacePath = "C:\\Users\\Arslek\\Projects\\ProjectMemory";
        
        DockerExecutionAdapter.ExecutionResult result = dockerAdapter.executeInContainer(workspacePath, image, command);
        
        run.setExitCode(result.exitCode());
        run.setStdoutLog(result.output());
        run.setStatus(ExecutionRunStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        
        // Create Evidence
        Evidence evidence = new Evidence();
        evidence.setExecutionRun(run);
        evidence.setEvidenceType("command_output");
        evidence.setTitle("Build Command Execution");
        evidence.setOutcome(result.outcome());
        evidence.setContent(result.output());
        
        run.getEvidences().add(evidence);
        executionRunRepository.save(run);

        // Update task status
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        log.info("Execution complete with exit code {}. Status moved to COMPLETED.", result.exitCode());
        return run;
    }
}
