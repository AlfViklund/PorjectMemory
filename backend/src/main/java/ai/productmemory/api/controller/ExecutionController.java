package ai.productmemory.api.controller;

import ai.productmemory.service.ExecutionRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionRunService executionRunService;

    @PostMapping("/start")
    public ResponseEntity<Void> startExecution(@PathVariable UUID taskId) {
        executionRunService.startExecution(taskId);
        return ResponseEntity.ok().build();
    }
}
