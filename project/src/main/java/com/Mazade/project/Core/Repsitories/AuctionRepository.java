package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Auction findByCategoryAndStatus(Category category, AuctionStatus status);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findByCategoryAndStatusIn(Category category, List<AuctionStatus> statuses);
}