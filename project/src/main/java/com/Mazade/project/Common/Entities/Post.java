package com.Mazade.project.Common.Entities;

import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "post")
public class Post extends BaseEntity {
    @Column(name = "title", nullable = false)
    @NotNull(message = "Title cannot be blank")
    private String title;

    @Column(name = "description", nullable = false)
    @NotNull(message = "Description cannot be blank")
    private String description;

    @Column(name = "startPrice", nullable = false)
    @NotNull(message = "Price cannot be blank")
    private double startPrice;

    @Column(name = "Category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "media", nullable = false, columnDefinition = "TEXT")
    @NotNull(message = "Media cannot be blank")
    private String media;

    @Column(name = "bidStep", nullable = false)
    @NotNull(message = "Bid step cannot be blank")
    private double bidStep;

    @Column(name = "status", columnDefinition = "VARCHAR(10) CHECK (status IN ('WAITING','IN_PROGRESS','COMPLETED'))")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "winnerId")
    private Long winnerId;

    @Column(name = "finalPrice")
    private double finalPrice;

    @Column(name = "viewCount")
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "isAccepted")
    @Builder.Default
    @JsonIgnore
    private boolean isAccepted = false;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    @NotNull(message = "User is required")
    @JsonBackReference("userPosts")
    private User user;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "postId", referencedColumnName = "id")
    @JsonManagedReference("postBid")
    private List<AutoBid> autoBids;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "postId", referencedColumnName = "id")
    @JsonManagedReference("postInteresteds")
    private List<Interested> interesteds;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "postId", referencedColumnName = "id")
    @JsonManagedReference("postBidTrackers")
    private List<AuctionBidTracker>  auctionBidTrackers;

    @ManyToOne
    @JoinColumn(name = "auctionId", nullable = false)
    @JsonBackReference("postAuction")
    private Auction auction;
}
