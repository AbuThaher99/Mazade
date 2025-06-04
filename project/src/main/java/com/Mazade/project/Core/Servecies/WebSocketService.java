
package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.BidUpdateDTO;
import com.Mazade.project.Common.DTOs.TimerNotification;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Enhanced bid update notification with proper DTO structure
     */
    public void notifyBidUpdate(Post post, Long userId, double amount) {
        try {
            // Create proper BidUpdateDTO
            BidUpdateDTO bidUpdateDTO = new BidUpdateDTO(
                    post.getId().longValue(),
                    post.getFinalPrice(), // Use the updated final price
                    "user-" + userId,
                    java.time.LocalDateTime.now().format(formatter)
            );

            log.info("📤 Sending bid update: Post {}, Final Price: {}, User: {}",
                    post.getId(), post.getFinalPrice(), userId);

            // Send to post-specific subscribers
            messagingTemplate.convertAndSend("/topic/auction/" + post.getId() + "/bids", bidUpdateDTO);

            // Send to auction-wide subscribers
            messagingTemplate.convertAndSend("/topic/auction/" + post.getAuction().getId(), bidUpdateDTO);

            log.info("✅ Bid update sent successfully");

        } catch (Exception e) {
            log.error("❌ Error sending bid update notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Enhanced bid tracker update notification
     */
    public void notifyBidTrackerUpdate(Long postId, AuctionBidTracker tracker) {
        try {
            log.info("📤 Sending bid tracker update for post {}", postId);

            messagingTemplate.convertAndSend("/topic/auction/" + postId + "/trackers", tracker);

            log.info("✅ Bid tracker update sent successfully");

        } catch (Exception e) {
            log.error("❌ Error sending bid tracker update: {}", e.getMessage(), e);
        }
    }

    /**
     * Enhanced timer notifications with detailed logging
     */
    public void sendTimerNotification(Long postId, TimerNotification notification) {
        try {
            log.info("📤 Sending timer notification: Post {}, Event: {}, Remaining: {}",
                    postId, notification.getEvent(), notification.getRemainingSeconds());

            // Send to post-specific timer subscribers
            messagingTemplate.convertAndSend("/topic/auction/" + postId + "/timer", notification);

            // Also send to general auction subscribers
            messagingTemplate.convertAndSend("/topic/auction/" + postId, notification);

            log.info("✅ Timer notification sent successfully");

        } catch (Exception e) {
            log.error("❌ Error sending timer notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send auction-wide notifications
     */
    public void sendAuctionNotification(Long auctionId, Object notification) {
        try {
            log.info("📤 Sending auction-wide notification for auction {}", auctionId);

            messagingTemplate.convertAndSend("/topic/auction/" + auctionId, notification);

            log.info("✅ Auction notification sent successfully");

        } catch (Exception e) {
            log.error("❌ Error sending auction notification: {}", e.getMessage(), e);
        }
    }
}