package ai.productmemory.repository;

import ai.productmemory.domain.entity.ProductMemoryItem;
import ai.productmemory.domain.enums.MemoryItemStatus;
import ai.productmemory.domain.enums.ProductMemoryItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductMemoryItemRepository extends JpaRepository<ProductMemoryItem, UUID> {
    Page<ProductMemoryItem> findByWorkspaceId(UUID workspaceId, Pageable pageable);
    List<ProductMemoryItem> findByWorkspaceIdAndItemType(UUID workspaceId, ProductMemoryItemType type);
    List<ProductMemoryItem> findByWorkspaceIdAndStatus(UUID workspaceId, MemoryItemStatus status);
    List<ProductMemoryItem> findByWorkspaceIdAndItemTypeIn(UUID workspaceId, List<ProductMemoryItemType> types);

    @Query("SELECT pmi FROM ProductMemoryItem pmi LEFT JOIN FETCH pmi.outgoingLinks WHERE pmi.id = :id")
    java.util.Optional<ProductMemoryItem> findByIdWithLinks(UUID id);

    @Query("""
        SELECT DISTINCT pmi FROM ProductMemoryItem pmi
        WHERE pmi.workspace.id = :workspaceId
        AND LOWER(pmi.name) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY pmi.name
        """)
    List<ProductMemoryItem> searchByName(UUID workspaceId, String query, Pageable pageable);

    boolean existsByWorkspaceIdAndItemTypeAndNameAndGroundingRef(
            UUID workspaceId, ProductMemoryItemType itemType, String name, String groundingRef);
}
