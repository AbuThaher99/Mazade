package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Auction findByCategoryAndStatus(Category category, AuctionStatus status);
    List<Auction> findByStatus(AuctionStatus status);


    @Query("SELECT a FROM Auction a WHERE (:status IS NULL OR a.status = :status)")
    Page<Auction> findAllWithFilters(Pageable pageable, @Param("status") AuctionStatus status);
}