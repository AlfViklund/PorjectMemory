package ai.productmemory.repository;

import ai.productmemory.domain.entity.MemoryLink;
import ai.productmemory.domain.enums.MemoryLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryLinkRepository extends JpaRepository<MemoryLink, UUID> {
    List<MemoryLink> findBySourceItemId(UUID sourceItemId);
    List<MemoryLink> findByTargetItemId(UUID targetItemId);
    List<MemoryLink> findBySourceItemIdAndLinkType(UUID sourceItemId, MemoryLinkType linkType);

    @Query("""
        SELECT ml FROM MemoryLink ml
        WHERE ml.sourceItem.id = :itemId OR ml.targetItem.id = :itemId
        """)
    List<MemoryLink> findAllRelatedLinks(UUID itemId);

    void deleteBySourceItemIdAndTargetItemIdAndLinkType(UUID sourceId, UUID targetId, MemoryLinkType type);
}
