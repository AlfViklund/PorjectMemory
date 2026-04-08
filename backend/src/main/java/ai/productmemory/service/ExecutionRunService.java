package ai.productmemory.service;

import ai.productmemory.domain.entity.CapabilityReportStatus;
import ai.productmemory.domain.entity.RuntimeCapabilityReport;
import ai.productmemory.domain.entity.Task;
import ai.productmemory.repository.RuntimeCapabilityReportRepository;
import ai.productmemory.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionRunService {

    private final TaskRepository taskRepository;
    private final RuntimeCapabilityReportRepository reportRepository;

    @Transactional
    public void startExecution(UUID taskId) {
        log.info("Attempting to start execution for task {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        UUID workspaceId = task.getChangeRequest().getWorkspace().getId();

        // ------------------------------------------------------------------
        // STOP-GATE ENFORCEMENT: Capability Research
        // ------------------------------------------------------------------
        RuntimeCapabilityReport report = reportRepository.findTopByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new IllegalStateException(
                        "STOP-GATE: No Capability Report found. You must run Capability Probe first."
                ));

        if (report.getStatus() != CapabilityReportStatus.APPROVED) {
            throw new IllegalStateException(
                    "STOP-GATE: Capability Report is not APPROVED (current status: " + report.getStatus() + ")."
            );
        }

        // TODO: Phase 9 -> initialize DockerContainerAdapter and execute task context
        log.info("Stop-gate passed! Execution can proceed using report commands: {}", report.getReportJsonb().buildCommands());
    }
}
