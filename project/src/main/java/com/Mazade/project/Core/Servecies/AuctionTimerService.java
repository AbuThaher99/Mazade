package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.TimerNotification;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Core.Repsitories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionTimerService {

    private final WebSocketService webSocketService;
    private final PostService postService;
    private final PostRepository postRepository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20);
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDateTime> timerStartTimes = new ConcurrentHashMap<>();

    // NEW: Track posts in delay phase (between auctions)
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> delayTimers = new ConcurrentHashMap<>();

    // Timer duration in seconds (30 seconds)
    private static final int TIMER_DURATION_SECONDS = 30;
    // NEW: Delay between auctions in seconds (10 seconds)
    private static final int AUCTION_DELAY_SECONDS = 10;

    /**
     * Start sequential auction processing - only start the FIRST post
     */
    public void startSequentialAuction(Auction auction) {
        if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
            log.warn("Cannot start sequential auction for ID: {} - Auction not in progress", auction.getId());
            return;
        }

        // Get all posts for this auction ordered by ID (or creation time)
        List<Post> auctionPosts = postRepository.findByAuctionOrderByIdAsc(auction);

        if (auctionPosts.isEmpty()) {
            log.warn("No posts found for auction ID: {}", auction.getId());
            return;
        }

        log.info("🎯 Starting sequential auction ID: {} with {} posts", auction.getId(), auctionPosts.size());

        // Start with the FIRST post only
        Post firstPost = auctionPosts.get(0);
        startNextPostInSequence(auction.getId(), firstPost.getId());
    }

    /**
     * Start the next post in the auction sequence
     */
    public void startNextPostInSequence(Long auctionId, Long postId) {
        try {
            // Update post status to IN_PROGRESS
            Post post = postService.updatePostStatus(postId, Status.IN_PROGRESS);

            log.info("🚀 Starting post {} in auction {} sequence", postId, auctionId);
            log.info("   - Post Title: {}", post.getTitle());
            log.info("   - Starting Price: {} NIS", post.getStartPrice());
            log.info("   - Bid Step: {} NIS", post.getBidStep());

            // Start 30-second timer for this post
            startPostTimer(postId);

            // Notify via WebSocket that this post is now active
            notifyPostStarted(postId, post);

        } catch (Exception e) {
            log.error("Error starting next post {} in auction {}: {}", postId, auctionId, e.getMessage(), e);
        }
    }

    /**
     * Start timer for a specific post
     */
    public void startPostTimer(Long postId) {
        // Stop any existing timer for this post
        stopPostTimer(postId);

        try {
            Post post = postService.getPostById(postId);
            if (post == null) {
                log.warn("Cannot start timer for post ID: {} - Post not found", postId);
                return;
            }

            log.info("⏰ Starting 30-second timer for post ID: {} ({})", postId, post.getTitle());

            // Record start time
            timerStartTimes.put(postId, LocalDateTime.now());

            // Schedule the timer task
            ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
                handlePostTimerExpired(postId);
            }, TIMER_DURATION_SECONDS, TimeUnit.SECONDS);

            // Store the timer reference
            activeTimers.put(postId, timerTask);

            // Notify via WebSocket that timer started
            notifyTimerStarted(postId);

        } catch (Exception e) {
            log.error("Error starting timer for post ID: {}", postId, e);
        }
    }

    /**
     * Restart timer for a specific post (used when new bid is placed)
     */
    public void restartPostTimer(Long postId) {
        log.info("🔄 Restarting timer for post ID: {} due to new bid", postId);

        // Stop current timer if exists
        stopPostTimer(postId);

        // Start new 30-second timer
        try {
            Post post = postService.getPostById(postId);
            if (post == null || post.getStatus() != Status.IN_PROGRESS) {
                log.warn("Cannot restart timer for post ID: {} - Post not found or not in progress", postId);
                return;
            }

            // Record new start time
            timerStartTimes.put(postId, LocalDateTime.now());

            // Schedule new timer task
            ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
                handlePostTimerExpired(postId);
            }, TIMER_DURATION_SECONDS, TimeUnit.SECONDS);

            // Store the timer reference
            activeTimers.put(postId, timerTask);

            // Notify about timer restart
            notifyTimerRestarted(postId);

        } catch (Exception e) {
            log.error("Error restarting timer for post ID: {}", postId, e);
        }
    }

    /**
     * Handle when a post timer expires - move to next post in sequence WITH DELAY
     */
    private void handlePostTimerExpired(Long postId) {
        try {
            log.info("🔥 TIMER EXPIRED for post ID: {} at {}", postId, LocalDateTime.now());

            // Remove from active timers
            activeTimers.remove(postId);
            timerStartTimes.remove(postId);

            // Get post details for logging
            Post completedPost = postService.getPostById(postId);
            if (completedPost == null) {
                log.error("Post not found when timer expired: {}", postId);
                return;
            }

            // Log auction summary for completed post
            logPostAuctionSummary(completedPost);

            // Mark current post as COMPLETED
            postService.updatePostStatus(postId, Status.COMPLETED);
            log.info("✅ Post ID: {} marked as COMPLETED", postId);

            // Set the winner ID for the post (if applicable)
            postService.getHighestBidderId(postId);

            // Notify clients that this post's auction ended
            notifyTimerExpired(postId);

            // Find the next post in the auction
            Long auctionId = completedPost.getAuction().getId();
            Post nextPost = findNextPostInAuction(auctionId);

            if (nextPost != null) {
                log.info("⏳ Starting 10-second delay before next post in auction {}: {} ({})",
                        auctionId, nextPost.getId(), nextPost.getTitle());

                // NEW: Start delay phase with countdown
                startAuctionDelay(postId, auctionId, nextPost);

            } else {
                log.info("🏁 No more posts in auction {}. Auction sequence completed!", auctionId);
                handleAuctionSequenceCompleted(auctionId);
            }

        } catch (Exception e) {
            log.error("Error handling timer expiration for post ID: {}", postId, e);
        }
    }

    /**
     * NEW: Start the delay phase between auctions
     */
    private void startAuctionDelay(Long completedPostId, Long auctionId, Post nextPost) {
        try {
            // Notify that delay phase started
            notifyAuctionDelayStarted(completedPostId, nextPost.getTitle());

            // Schedule countdown notifications (optional - every 2 seconds)
            for (int i = 8; i >= 2; i -= 2) {
                final int remainingSeconds = i;
                scheduler.schedule(() -> {
                    notifyAuctionDelayCountdown(completedPostId, remainingSeconds, nextPost.getTitle());
                }, AUCTION_DELAY_SECONDS - i, TimeUnit.SECONDS);
            }

            // Schedule the main delay task
            ScheduledFuture<?> delayTask = scheduler.schedule(() -> {
                handleDelayCompleted(auctionId, nextPost.getId());
            }, AUCTION_DELAY_SECONDS, TimeUnit.SECONDS);

            // Store the delay timer reference
            delayTimers.put(completedPostId, delayTask);

        } catch (Exception e) {
            log.error("Error starting auction delay for post ID: {}", completedPostId, e);
        }
    }

    /**
     * NEW: Handle when the delay phase is completed
     */
    private void handleDelayCompleted(Long auctionId, Long nextPostId) {
        try {
            log.info("✅ Auction delay completed. Starting next post: {}", nextPostId);

            // Remove from delay timers
            delayTimers.remove(nextPostId);

            // Notify that delay is over and next auction is starting
            notifyAuctionDelayCompleted(nextPostId);

            // Start the next post in sequence
            startNextPostInSequence(auctionId, nextPostId);

        } catch (Exception e) {
            log.error("Error handling delay completion for next post ID: {}", nextPostId, e);
        }
    }

    /**
     * Find the next WAITING post in the auction sequence
     */
    private Post findNextPostInAuction(Long auctionId) {
        try {
            // Get all posts in auction ordered by ID
            List<Post> auctionPosts = postRepository.findByAuctionIdOrderByIdAsc(auctionId);

            // Find the first post with WAITING status
            return auctionPosts.stream()
                    .filter(post -> post.getStatus() == Status.WAITING)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("Error finding next post in auction {}: {}", auctionId, e.getMessage());
            return null;
        }
    }

    /**
     * Handle when all posts in an auction sequence are completed
     */
    private void handleAuctionSequenceCompleted(Long auctionId) {
        try {
            log.info("🎊 Auction sequence {} completed successfully!", auctionId);

            // Get auction completion stats
            List<Post> allAuctionPosts = postRepository.findByAuctionIdOrderByIdAsc(auctionId);

            int totalPosts = allAuctionPosts.size();
            int completedPosts = (int) allAuctionPosts.stream()
                    .filter(post -> post.getStatus() == Status.COMPLETED)
                    .count();

            double totalRevenue = allAuctionPosts.stream()
                    .mapToDouble(post -> post.getFinalPrice() != 0.0 ? post.getFinalPrice() : 0.0)
                    .sum();

            log.info("📊 Auction {} Final Summary:", auctionId);
            log.info("   - Total Posts: {}", totalPosts);
            log.info("   - Completed Posts: {}", completedPosts);
            log.info("   - Total Revenue: {} NIS", totalRevenue);

            // Notify all clients that auction sequence is completed
            notifyAuctionSequenceCompleted(auctionId, totalPosts, completedPosts, totalRevenue);

        } catch (Exception e) {
            log.error("Error handling auction sequence completion for auction {}: {}", auctionId, e.getMessage());
        }
    }

    /**
     * Log detailed summary for a completed post - FIXED VERSION
     */
    private void logPostAuctionSummary(Post post) {
        try {
            // Use repository query to count bid trackers instead of accessing lazy collection
            long bidCount = postRepository.countAuctionBidTrackersByPostId(post.getId());

            log.info("📋 Post Auction Summary:");
            log.info("   - Post ID: {}", post.getId());
            log.info("   - Title: {}", post.getTitle());
            log.info("   - Starting Price: {} NIS", post.getStartPrice());
            log.info("   - Final Price: {} NIS", post.getFinalPrice() != 0.0? post.getFinalPrice() : post.getStartPrice());
            log.info("   - Total Bids: {}", bidCount);
            log.info("   - Auction ID: {}", post.getAuction().getId());

            if (post.getFinalPrice() != 0.0 && post.getFinalPrice() > post.getStartPrice()) {
                double profit = post.getFinalPrice() - post.getStartPrice();
                log.info("   - Profit: +{} NIS", profit);
            }
        } catch (Exception e) {
            log.error("Error logging post auction summary for post ID: {}", post.getId(), e);
        }
    }

    /**
     * Stop timer for a specific post
     */
    public void stopPostTimer(Long postId) {
        ScheduledFuture<?> timer = activeTimers.remove(postId);
        if (timer != null) {
            timer.cancel(false);
            timerStartTimes.remove(postId);
            log.info("⏹️ Timer stopped for post ID: {}", postId);
            notifyTimerStopped(postId);
        }

        // NEW: Also stop any delay timer
        ScheduledFuture<?> delayTimer = delayTimers.remove(postId);
        if (delayTimer != null) {
            delayTimer.cancel(false);
            log.info("⏹️ Delay timer stopped for post ID: {}", postId);
        }
    }

    /**
     * Stop all timers for an auction (emergency stop)
     */
    public void stopAuctionTimers(Long auctionId) {
        try {
            List<Post> auctionPosts = postRepository.findByAuctionId(auctionId);

            log.info("🛑 Emergency stop: Stopping timers for {} posts in auction ID: {}", auctionPosts.size(), auctionId);

            for (Post post : auctionPosts) {
                stopPostTimer(post.getId());
            }
        } catch (Exception e) {
            log.error("Error stopping auction timers for auction ID: {}", auctionId, e);
        }
    }

    /**
     * Get remaining time for a post timer
     */
    public long getRemainingTimeSeconds(Long postId) {
        LocalDateTime startTime = timerStartTimes.get(postId);
        if (startTime == null || !activeTimers.containsKey(postId)) {
            return 0;
        }

        long elapsedSeconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        long remainingSeconds = TIMER_DURATION_SECONDS - elapsedSeconds;
        return Math.max(0, remainingSeconds);
    }

    /**
     * Check if timer is active for a post
     */
    public boolean isTimerActive(Long postId) {
        return activeTimers.containsKey(postId);
    }

    /**
     * NEW: Check if post is in delay phase
     */
    public boolean isInDelayPhase(Long postId) {
        return delayTimers.containsKey(postId);
    }

    /**
     * Get all active timer post IDs
     */
    public java.util.Set<Long> getActiveTimerPostIds() {
        return activeTimers.keySet();
    }

    /**
     * Get current active post in an auction (the one with timer running)
     */
    public Long getCurrentActivePostInAuction(Long auctionId) {
        try {
            List<Post> auctionPosts = postRepository.findByAuctionId(auctionId);

            return auctionPosts.stream()
                    .filter(post -> post.getStatus() == Status.IN_PROGRESS && isTimerActive(post.getId()))
                    .map(Post::getId)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("Error getting current active post in auction {}: {}", auctionId, e.getMessage());
            return null;
        }
    }

    // WebSocket Notification Methods

    private void notifyTimerStarted(Long postId) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("TIMER_STARTED")
                    .remainingSeconds(TIMER_DURATION_SECONDS)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Post auction started - 30 seconds remaining")
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending timer start notification for post ID: {}", postId, e);
        }
    }

    private void notifyTimerRestarted(Long postId) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("TIMER_RESTARTED")
                    .remainingSeconds(TIMER_DURATION_SECONDS)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Timer restarted due to new bid - 30 seconds remaining")
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending timer restart notification for post ID: {}", postId, e);
        }
    }

    private void notifyTimerStopped(Long postId) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("TIMER_STOPPED")
                    .remainingSeconds(0)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Timer stopped")
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending timer stop notification for post ID: {}", postId, e);
        }
    }

    private void notifyTimerExpired(Long postId) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("TIMER_EXPIRED")
                    .remainingSeconds(0)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Auction ended for this item")
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending timer expiration notification for post ID: {}", postId, e);
        }
    }

    private void notifyPostStarted(Long postId, Post post) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("POST_STARTED")
                    .remainingSeconds(TIMER_DURATION_SECONDS)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Now auctioning: " + post.getTitle())
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending post start notification for post ID: {}", postId, e);
        }
    }

    // NEW: Delay phase notification methods
    private void notifyAuctionDelayStarted(Long postId, String nextPostTitle) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("AUCTION_DELAY_STARTED")
                    .remainingSeconds(AUCTION_DELAY_SECONDS)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Auction completed! Next item: " + nextPostTitle + " (starting in 10 seconds)")
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending auction delay start notification for post ID: {}", postId, e);
        }
    }

    private void notifyAuctionDelayCountdown(Long postId, int remainingSeconds, String nextPostTitle) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(postId)
                    .event("AUCTION_DELAY_COUNTDOWN")
                    .remainingSeconds(remainingSeconds)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Next item: " + nextPostTitle + " (starting in " + remainingSeconds + " seconds)")
                    .build();

            webSocketService.sendTimerNotification(postId, notification);
        } catch (Exception e) {
            log.error("Error sending auction delay countdown notification for post ID: {}", postId, e);
        }
    }

    private void notifyAuctionDelayCompleted(Long nextPostId) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(nextPostId)
                    .event("AUCTION_DELAY_COMPLETED")
                    .remainingSeconds(0)
                    .timestamp(LocalDateTime.now().toString())
                    .message("Starting next auction now!")
                    .build();

            webSocketService.sendTimerNotification(nextPostId, notification);
        } catch (Exception e) {
            log.error("Error sending auction delay completion notification for post ID: {}", nextPostId, e);
        }
    }

    private void notifyAuctionSequenceCompleted(Long auctionId, int totalPosts, int completedPosts, double totalRevenue) {
        try {
            TimerNotification notification = TimerNotification.builder()
                    .postId(auctionId) // Using auction ID as post ID for this notification
                    .event("AUCTION_SEQUENCE_COMPLETED")
                    .remainingSeconds(0)
                    .timestamp(LocalDateTime.now().toString())
                    .message(String.format("Auction completed! %d/%d posts sold. Total: %.2f NIS",
                            completedPosts, totalPosts, totalRevenue))
                    .build();

            webSocketService.sendTimerNotification(auctionId, notification);
        } catch (Exception e) {
            log.error("Error sending auction sequence completion notification for auction ID: {}", auctionId, e);
        }
    }

    /**
     * Shutdown the timer service
     */
    public void shutdown() {
        log.info("Shutting down auction timer service...");
        activeTimers.values().forEach(timer -> timer.cancel(false));
        activeTimers.clear();

        // NEW: Cancel delay timers too
        delayTimers.values().forEach(timer -> timer.cancel(false));
        delayTimers.clear();

        timerStartTimes.clear();
        scheduler.shutdown();
    }

    public Serializable getActiveTimerCount() {
        return activeTimers.size();
    }
}