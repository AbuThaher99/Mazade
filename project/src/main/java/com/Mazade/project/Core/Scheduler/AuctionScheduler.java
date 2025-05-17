package com.Mazade.project.Core.Scheduler;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Core.Repsitories.AuctionRepository;
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

    /**
     * Scheduled task to open auctions.
     * Runs at 6:00 PM every Monday and Thursday
     */
    @Scheduled(cron = "0 0 18 * * MON,THU")
    @Transactional
    public void openAuctions() {
        List<Auction> waitingAuctions = auctionRepository.findByStatus(AuctionStatus.WAITING);
        int updatedCount = 0;

        for (Auction auction : waitingAuctions) {
            // Only open auctions that have posts
            if (auction.getPostCount() > 0) {
                auction.setStatus(AuctionStatus.IN_PROGRESS);
                updatedCount++;
            }
        }

        if (!waitingAuctions.isEmpty()) {
            auctionRepository.saveAll(waitingAuctions);
            log.info("Opened {} auctions at {}", updatedCount,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
}