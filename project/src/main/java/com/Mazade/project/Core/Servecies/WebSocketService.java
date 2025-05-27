package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.BidUpdateDTO;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void notifyBidUpdate(Post post, Long userId, double amount) {
        BidUpdateDTO bidUpdateDTO = new BidUpdateDTO(
                post.getId(),
                post.getFinalPrice(),
                "user-" + userId,
                java.time.LocalDateTime.now().format(formatter)
        );

        // Send to post-specific subscribers
        messagingTemplate.convertAndSend("/topic/auction/" + post.getId() + "/bids", bidUpdateDTO);

        // Send to auction-wide subscribers
        messagingTemplate.convertAndSend("/topic/auction/" + post.getAuction().getId(), bidUpdateDTO);
    }

    public void notifyBidTrackerUpdate(Long postId, AuctionBidTracker tracker) {
        messagingTemplate.convertAndSend("/topic/auction/" + postId + "/trackers", tracker);
    }
}