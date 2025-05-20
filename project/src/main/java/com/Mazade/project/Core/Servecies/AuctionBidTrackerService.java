package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Core.Repsitories.AuctionBidTrackerRepository;
import com.Mazade.project.Core.Repsitories.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionBidTrackerService {

    private final AuctionBidTrackerRepository bidTrackerRepository;
    @Transactional
    public void trackBid(Auction auction, Post post, Long userId, double bidAmount) {
        if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
            return;
        }
        String userIdentifier = "user-" + userId;

        AuctionBidTracker bidTracker = AuctionBidTracker.builder()
                .auction(auction)
                .post(post)
                .userIdentifier(userIdentifier)
                .bidAmount(bidAmount)
                .timestamp(LocalDateTime.now())
                .build();

        bidTrackerRepository.save(bidTracker);
    }
}