package com.Mazade.project.Common.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction_bid_tracker")
public class AuctionBidTracker extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "auctionId", nullable = false)
    @NotNull(message = "Auction is required")
    @JsonBackReference("actionBidTrackers")
    private Auction auction;

    @ManyToOne
    @JoinColumn(name = "postId", nullable = false)
    @NotNull(message = "Post is required")
    @JsonBackReference("postBidTrackers")
    private Post post;

    @Column(name = "userIdentifier", nullable = false)
    private String userIdentifier;  // Format: "user-userId"

    @Column(name = "bidAmount", nullable = false)
    private double bidAmount;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}