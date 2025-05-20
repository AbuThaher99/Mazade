package com.Mazade.project.Common.Entities;

import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction")
public class Auction extends BaseEntity {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "auctionId", referencedColumnName = "id")
    @JsonManagedReference("postAuction")
    private List<Post> posts;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "auctionId", referencedColumnName = "id")
    @JsonManagedReference("actionBidTrackers")
    private List<AuctionBidTracker>  auctionBidTrackers;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "postCount", nullable = false)
    @Builder.Default
    private Integer postCount = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.WAITING;
}