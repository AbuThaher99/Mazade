package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Core.Repsitories.AuctionBidTrackerRepository;
import com.Mazade.project.Core.Repsitories.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionBidTrackerService {

    private final AuctionBidTrackerRepository bidTrackerRepository;
    private final PostRepository postRepository;
    @Transactional
    public AuctionBidTracker trackBid(Auction auction, Post post, Long userId, double bidAmount) {
        if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
            return null;
        }
        String userIdentifier = "user-" + userId;

        AuctionBidTracker bidTracker = AuctionBidTracker.builder()
                .auction(auction)
                .post(post)
                .userIdentifier(userIdentifier)
                .bidAmount(bidAmount)
                .timestamp(LocalDateTime.now())
                .build();

        AuctionBidTracker savedTracker = bidTrackerRepository.save(bidTracker);

        // Broadcasting is now handled in the controller
        return savedTracker;
    }

    @Transactional
    public PaginationDTO<AuctionBidTracker> getBidTrackersByPostId(Long postId, int page, int size) {
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<AuctionBidTracker> bidTrackersPage = bidTrackerRepository.findByPostIdOrderByTimestampDesc(postId, pageable);

        PaginationDTO<AuctionBidTracker> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(bidTrackersPage.getTotalElements());
        paginationDTO.setTotalPages(bidTrackersPage.getTotalPages());
        paginationDTO.setSize(bidTrackersPage.getSize());
        paginationDTO.setNumber(bidTrackersPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(bidTrackersPage.getNumberOfElements());
        paginationDTO.setContent(bidTrackersPage.getContent());

        return paginationDTO;
    }
}