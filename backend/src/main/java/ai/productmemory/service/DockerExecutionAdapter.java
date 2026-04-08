package ai.productmemory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DockerExecutionAdapter {

    public ExecutionResult executeInContainer(String workspacePath, String image, String command) {
        log.info("Executing Docker run for image {} at path {}", image, workspacePath);

        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add("docker");
        cmdArgs.add("run");
        cmdArgs.add("--rm");
        cmdArgs.add("-v");
        cmdArgs.add(workspacePath + ":/workspace");
        cmdArgs.add("-w");
        cmdArgs.add("/workspace");
        cmdArgs.add(image);
        cmdArgs.add("/bin/sh");
        cmdArgs.add("-c");
        cmdArgs.add(command);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("[DOCKER] {}", line);
                }
            }

            int exitCode = process.waitFor();
            boolean isSuccess = exitCode == 0;
            return new ExecutionResult(exitCode, output.toString(), isSuccess ? "PASS" : "FAIL");
        } catch (Exception e) {
            log.error("Docker execution failed", e);
            return new ExecutionResult(-1, "Executor Error: " + e.getMessage(), "FAIL");
        }
    }

    public record ExecutionResult(int exitCode, String output, String outcome) {}
}
