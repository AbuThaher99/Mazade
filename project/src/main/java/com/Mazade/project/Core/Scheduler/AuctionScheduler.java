package com.Mazade.project.Core.Scheduler;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Core.Repsitories.AuctionRepository;
import com.Mazade.project.Core.Servecies.AuctionTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionTimerService auctionTimerService;

    /**
     * Scheduled task to open auctions and start sequential processing.
     * Runs at 6:00 PM every Monday and Thursday And WENESDAY
     */
    @Scheduled(cron = "0 11 14 * * MON,THU,WED")
    @Transactional
    public void openAuctionsAndStartSequence() {
        List<Auction> waitingAuctions = auctionRepository.findByStatus(AuctionStatus.WAITING);
        int updatedCount = 0;

        log.info("🕕 Auction opening time! Found {} waiting auctions", waitingAuctions.size());

        for (Auction auction : waitingAuctions) {
            // Only open auctions that have posts
            if (auction.getPostCount() > 0) {
                auction.setStatus(AuctionStatus.IN_PROGRESS);
                updatedCount++;

                log.info("🎯 Opening auction ID: {} with {} posts", auction.getId(), auction.getPostCount());

                // Start sequential processing (only first post)
                try {
                    auctionTimerService.startSequentialAuction(auction);
                } catch (Exception e) {
                    log.error("Error starting sequential auction for auction ID: {}", auction.getId(), e);
                }
            } else {
                log.warn("⚠️ Skipping auction ID: {} - No posts available", auction.getId());
            }
        }

        if (!waitingAuctions.isEmpty()) {
            auctionRepository.saveAll(waitingAuctions);
            log.info("✅ Successfully opened {} auctions at {}", updatedCount,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            log.info("ℹ️ No auctions to open at {}",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    /**
     * Scheduled task to clean up completed auctions
     * Runs every hour to check if all posts in auctions are completed
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupCompletedAuctions() {
        try {
            List<Auction> inProgressAuctions = auctionRepository.findByStatus(AuctionStatus.IN_PROGRESS);

            for (Auction auction : inProgressAuctions) {
                // Check if all posts in auction are completed
                boolean allPostsCompleted = auction.getPosts().stream()
                        .allMatch(post -> post.getStatus().toString().equals("COMPLETED"));

                if (allPostsCompleted && auction.getPosts().size() > 0) {
                    auction.setStatus(AuctionStatus.COMPLETED);
                    auctionTimerService.stopAuctionTimers(auction.getId());
                    log.info("🏁 Completed auction ID: {} - All posts finished", auction.getId());
                }
            }

            if (!inProgressAuctions.isEmpty()) {
                auctionRepository.saveAll(inProgressAuctions);
            }

        } catch (Exception e) {
            log.error("Error in auction cleanup task", e);
        }
    }
}