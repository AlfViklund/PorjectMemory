package ai.productmemory.repository;

import ai.productmemory.domain.entity.Review;
import ai.productmemory.domain.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByChangeRequestId(UUID changeRequestId);
    List<Review> findByStatus(ReviewStatus status);
    Optional<Review> findByShortId(String shortId);
}
