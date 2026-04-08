package ai.productmemory.repository;

import ai.productmemory.domain.entity.TaskContextPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskContextPackRepository extends JpaRepository<TaskContextPack, UUID> {
    Optional<TaskContextPack> findByTaskId(UUID taskId);
    Optional<TaskContextPack> findByShortId(String shortId);
}
