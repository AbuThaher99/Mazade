package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.AuctionBidTracker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionBidTrackerRepository extends JpaRepository<AuctionBidTracker, Long> {
    List<AuctionBidTracker> findByAuctionId(Long auctionId);
    Page<AuctionBidTracker> findByPostIdOrderByTimestampDesc(Long postId, Pageable pageable);
    Optional<AuctionBidTracker> findTopByPostIdOrderByCreatedDateDesc(Long postId);

}