package ai.productmemory.repository;

import ai.productmemory.domain.entity.ExecutionRun;
import ai.productmemory.domain.enums.ExecutionRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, UUID> {
    List<ExecutionRun> findByTaskId(UUID taskId);
    List<ExecutionRun> findByStatus(ExecutionRunStatus status);
    List<ExecutionRun> findByTaskIdOrderByStartedAtDesc(UUID taskId);
}
