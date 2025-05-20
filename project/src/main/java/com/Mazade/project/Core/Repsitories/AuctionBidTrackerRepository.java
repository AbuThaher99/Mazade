package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.AuctionBidTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionBidTrackerRepository extends JpaRepository<AuctionBidTracker, Long> {
    List<AuctionBidTracker> findByAuctionId(Long auctionId);
}