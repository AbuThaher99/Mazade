package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.isAccepted = true " +
            "and p.status = com.Mazade.project.Common.Enums.Status.WAITING and " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY p.createdDate DESC")
    Page<Post> SortByDate(Pageable pageable, @Param("search") String search, @Param("category") Category category);

    // Update the other methods similarly
    @Query("SELECT p FROM Post p WHERE p.isAccepted = true " +
            "and p.status = com.Mazade.project.Common.Enums.Status.WAITING and " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY p.startPrice ASC")
    Page<Post> SortByPrice(Pageable pageable, @Param("search") String search, @Param("category") Category category);

    @Query("SELECT p FROM Post p JOIN p.user u WHERE p.isAccepted = true " +
            "and p.status = com.Mazade.project.Common.Enums.Status.WAITING and " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY u.rating DESC")
    Page<Post> SortByRating(Pageable pageable, @Param("search") String search, @Param("category") Category category);

    @Query("SELECT p FROM Post p WHERE p.isAccepted = true " +
            "and p.status = com.Mazade.project.Common.Enums.Status.WAITING and " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category)")
    Page<Post> findAllPosts(Pageable pageable, @Param("search") String search, @Param("category") Category category);

    List<Post> findByAuctionId(Long auctionId);

    @Query("SELECT p FROM Post p WHERE p.isAccepted = true AND p.user.id = :userId AND (:category IS NULL OR p.category = :category) ORDER BY p.createdDate DESC")
    Page<Post> findByUserIdAndCategoryOrderByCreatedDateDesc(
            @Param("userId") Long userId,
            @Param("category") Category category,
            Pageable pageable
    );

    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.isAccepted = true")
    Optional<Post> findByIdAndAccepted(Long postId);

    @Query("SELECT p FROM Post p WHERE p.isAccepted = false")
    Page<Post> findAllToAccept(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isAccepted = true AND p.winnerId = :userId AND (:category IS NULL OR p.category = :category)")
    Page<Post> findAcceptedPostsByWinnerIdAndCategory(
            @Param("userId") Long userId,
            @Param("category") Category category,
            Pageable pageable
    );

    // Add to PostRepository.java
    @Query("SELECT p FROM Post p WHERE p.auction.id = :auctionId AND p.status IN :statuses " +
            "AND p.isAccepted = true AND p.auction.status = com.Mazade.project.Common.Enums.AuctionStatus.IN_PROGRESS " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY p.createdDate ASC")
    Page<Post> findPostsForActiveAuctionByStatuses(
            @Param("auctionId") Long auctionId,
            @Param("statuses") List<Status> statuses,
            @Param("category") Category category,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM Post p WHERE p.auction.id = :auctionId AND p.status = com.Mazade.project.Common.Enums.Status.IN_PROGRESS " +
            "AND p.isAccepted = true AND p.auction.status = com.Mazade.project.Common.Enums.AuctionStatus.IN_PROGRESS")
    long countInProgressPostsForAuction(@Param("auctionId") Long auctionId);

    List<Post> findByAuctionOrderByIdAsc(Auction auction);

    /**
     * Find all posts by auction ID ordered by ID (for sequential processing)
     */
    List<Post> findByAuctionIdOrderByIdAsc(Long auctionId);
    List<Post> findByAuctionIdAndStatusOrderByIdAsc(Long auctionId, Status status);

    /**
     * Count auction bid trackers for a specific post
     */
    @Query("SELECT COUNT(abt) FROM AuctionBidTracker abt WHERE abt.post.id = :postId")
    long countAuctionBidTrackersByPostId(@Param("postId") Long postId);
}