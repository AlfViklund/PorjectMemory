package ai.productmemory.repository;

import ai.productmemory.domain.entity.MemoryMergePlan;
import ai.productmemory.domain.enums.MergePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoryMergePlanRepository extends JpaRepository<MemoryMergePlan, UUID> {
    Optional<MemoryMergePlan> findByReviewId(UUID reviewId);
    Optional<MemoryMergePlan> findByShortId(String shortId);
    List<MemoryMergePlan> findByStatus(MergePlanStatus status);
}
