package com.Mazade.project.WebApi.Controllers.User;

import com.Mazade.project.Core.Servecies.PostService;
import com.Mazade.project.Core.Servecies.WebSocketService;
import com.Mazade.project.Core.Servecies.AuctionTimerService;
import com.Mazade.project.Core.Servecies.AuctionBidTrackerService;
import com.Mazade.project.Core.Servecies.AuthenticationService;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Common.Enums.AuctionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final PostService postService;
    private final WebSocketService webSocketService;
    private final AuctionTimerService auctionTimerService;
    private final AuctionBidTrackerService auctionBidTrackerService;
    private final AuthenticationService authenticationService;

    /**
     * Enhanced bid message handler with better error handling and notifications
     */
    @MessageMapping("/auction/{postId}/bid")
    @Transactional
    public void handleBidMessage(
            @DestinationVariable Long postId,
            @Payload Map<String, Object> bidData,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            log.info("📨 Received WebSocket bid for post {}: {}", postId, bidData);

            // Validate bid data
            if (!validateBidData(bidData, postId)) {
                return;
            }

            // Extract bid amount
            double amount = extractBidAmount(bidData);
            if (amount <= 0) {
                log.error("❌ Invalid bid amount: {}", amount);
                sendErrorToUser(headerAccessor, "Invalid bid amount");
                return;
            }

            // Get user from session
            Long userId = getUserIdFromSession(headerAccessor);
            if (userId == null) {
                log.error("❌ No user ID found in WebSocket session");
                sendErrorToUser(headerAccessor, "Authentication required");
                return;
            }

            // Verify post exists and is active
            Post currentPost = postService.getPostById(postId);
            if (currentPost == null) {
                log.error("❌ Post not found: {}", postId);
                sendErrorToUser(headerAccessor, "Post not found");
                return;
            }

            if (currentPost.getStatus() != Status.IN_PROGRESS) {
                log.warn("❌ Post {} is not currently active for bidding. Status: {}",
                        postId, currentPost.getStatus());
                sendErrorToUser(headerAccessor, "This item is not currently active for bidding");
                return;
            }

            // Validate bid amount against current price
            double currentPrice = currentPost.getFinalPrice() != 0 ?
                    currentPost.getFinalPrice() : currentPost.getStartPrice();
            double minimumBid = currentPrice + currentPost.getBidStep();

            if (amount < minimumBid) {
                log.warn("❌ Bid amount {} is below minimum required: {}", amount, minimumBid);
                sendErrorToUser(headerAccessor,
                        String.format("Minimum bid is %.2f NIS", minimumBid));
                return;
            }

            // Process the bid
            log.info("💰 Processing bid: Post {}, Amount {}, User {}", postId, amount, userId);

            Post updatedPost = postService.increasePostFinalPrice(postId, amount);
            User user = authenticationService.getUserById(userId);

            if (user != null && updatedPost.getAuction().getStatus() == AuctionStatus.IN_PROGRESS) {
                // Track the bid
                AuctionBidTracker tracker = auctionBidTrackerService.trackBid(
                        updatedPost.getAuction(), updatedPost, userId, amount);

                log.info("✅ Bid processed successfully. New price: {}", updatedPost.getFinalPrice());

                // Send notifications AFTER successful processing
                webSocketService.notifyBidUpdate(updatedPost, userId, amount);
                webSocketService.notifyBidTrackerUpdate(postId, tracker);

                // Restart timer to 30 seconds
                if (auctionTimerService.isTimerActive(postId)) {
                    auctionTimerService.restartPostTimer(postId);
                    log.info("🔄 Timer restarted to 30 seconds for post {} due to WebSocket bid by user {}",
                            postId, userId);
                } else {
                    auctionTimerService.startPostTimer(postId);
                    log.info("⏰ Started new timer for post {} due to WebSocket bid by user {}",
                            postId, userId);
                }

                // Send success confirmation to bidder
                sendSuccessToUser(headerAccessor,
                        String.format("Bid of %.2f NIS placed successfully!", amount));

                log.info("✅ WebSocket bid completed successfully: Post {}, Amount {}, User {}",
                        postId, amount, userId);
            } else {
                log.error("❌ Failed to process bid: User or auction validation failed");
                sendErrorToUser(headerAccessor, "Failed to process bid");
            }

        } catch (Exception e) {
            log.error("❌ Error processing WebSocket bid for post {}: {}", postId, e.getMessage(), e);
            sendErrorToUser(headerAccessor, "Internal error processing bid");
        }
    }

    /**
     * Handle connection events
     */
    @MessageMapping("/auction/connect")
    public void handleConnect(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Long userId = getUserIdFromSession(headerAccessor);
        log.info("🔗 WebSocket client connected: {} (User: {})", sessionId, userId);
    }

    /**
     * Handle disconnection events
     */
    @MessageMapping("/auction/disconnect")
    public void handleDisconnect(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Long userId = getUserIdFromSession(headerAccessor);
        log.info("🔌 WebSocket client disconnected: {} (User: {})", sessionId, userId);
    }

    // Helper methods

    private boolean validateBidData(Map<String, Object> bidData, Long expectedPostId) {
        if (bidData == null || bidData.isEmpty()) {
            log.error("❌ Empty bid data received");
            return false;
        }

        Object postIdObj = bidData.get("postId");
        if (postIdObj == null) {
            log.error("❌ No postId in bid data");
            return false;
        }

        Long providedPostId = Long.valueOf(postIdObj.toString());
        if (!providedPostId.equals(expectedPostId)) {
            log.error("❌ PostId mismatch: expected {}, got {}", expectedPostId, providedPostId);
            return false;
        }

        Object amountObj = bidData.get("amount");
        if (amountObj == null) {
            log.error("❌ No amount in bid data");
            return false;
        }

        return true;
    }

    private double extractBidAmount(Map<String, Object> bidData) {
        Object amountObj = bidData.get("amount");
        try {
            if (amountObj instanceof Number) {
                return ((Number) amountObj).doubleValue();
            } else {
                return Double.parseDouble(amountObj.toString());
            }
        } catch (NumberFormatException e) {
            log.error("❌ Invalid amount format: {}", amountObj);
            return -1;
        }
    }

    private Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Object userIdObj = headerAccessor.getSessionAttributes().get("userId");
        if (userIdObj != null) {
            try {
                return Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException e) {
                log.error("❌ Invalid userId format in session: {}", userIdObj);
            }
        }

        // For testing only - remove in production
        log.warn("⚠️ Using default user ID for WebSocket session - implement proper authentication!");
        return 1L;
    }

    @SendToUser("/queue/errors")
    private void sendErrorToUser(SimpMessageHeaderAccessor headerAccessor, String message) {
        String sessionId = headerAccessor.getSessionId();
        log.info("📤 Sending error to user session {}: {}", sessionId, message);
        // You can implement user-specific error messaging here
    }

    @SendToUser("/queue/success")
    private void sendSuccessToUser(SimpMessageHeaderAccessor headerAccessor, String message) {
        String sessionId = headerAccessor.getSessionId();
        log.info("📤 Sending success to user session {}: {}", sessionId, message);
        // You can implement user-specific success messaging here
    }
}