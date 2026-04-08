package ai.productmemory.repository;

import ai.productmemory.domain.entity.Task;
import ai.productmemory.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByChangeRequestId(UUID changeRequestId);
    List<Task> findByChangeRequestIdAndStatus(UUID changeRequestId, TaskStatus status);
    Optional<Task> findByShortId(String shortId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.contextPack WHERE t.id = :id")
    Optional<Task> findByIdWithContextPack(UUID id);
}
