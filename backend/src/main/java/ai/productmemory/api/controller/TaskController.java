package ai.productmemory.api.controller;

import ai.productmemory.domain.entity.ExecutionRun;
import ai.productmemory.domain.entity.Task;
import ai.productmemory.repository.ExecutionRunRepository;
import ai.productmemory.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/change-requests/{crId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;
    private final ExecutionRunRepository executionRunRepository;

    @GetMapping
    public ResponseEntity<List<TaskDto>> listTasks(@PathVariable UUID crId) {
        List<Task> tasks = taskRepository.findByChangeRequestId(crId);
        List<TaskDto> dtos = tasks.stream().map(t -> {
            List<ExecutionRun> runs = executionRunRepository.findByTaskId(t.getId());
            return new TaskDto(t, runs);
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    public record TaskDto(
            UUID id, 
            String status, 
            Object contextPack,
            List<RunDto> runs
    ) {
        public TaskDto(Task task, List<ExecutionRun> runs) {
            this(
                task.getId(), 
                task.getStatus().name(), 
                task.getContextPack() != null ? task.getContextPack().getPayload() : null,
                runs.stream().map(RunDto::new).collect(Collectors.toList())
            );
        }
    }
    
    public record RunDto(
            UUID id, 
            String status, 
            Integer exitCode, 
            String stdoutLog, 
            List<Object> evidences
    ) {
        public RunDto(ExecutionRun run) {
            this(
                run.getId(), 
                run.getStatus().name(), 
                run.getExitCode(), 
                run.getStdoutLog(),
                run.getEvidences().stream().map(e -> e.getContent()).collect(Collectors.toList())
            );
        }
    }
}
